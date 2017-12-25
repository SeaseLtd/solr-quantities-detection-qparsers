package com.spaziocodice.labs.solr.qty;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.ExtendedDismaxQParserPlugin;
import org.apache.solr.search.LuceneQParserPlugin;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

import static java.util.stream.Collectors.toMap;

/**
 * Supertype layer for detecting quantities in a query string.
 *
 * Note that the {@link QParserPlugin} and {@link org.apache.lucene.analysis.util.ResourceLoaderAware}
 * inheritance relationships have been declared here because we don't have multiple inheritance in Java.
 *
 * @author agazzarini
 * @since 1.0
 */
public abstract class QuantityDetector extends QParserPlugin implements ResourceLoaderAware {
    interface QueryBuilder {
        void newQuantityDetected(final QuantityOccurrence occurrence);

        String product();

        QParserPlugin qparserPlugin();
    }

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Map<String, String> variantsMap;
    protected Map<String, Object> configuration;

    /**
     * Completes the initialization of this component by loading the provided configuration.
     *
     * @param loader the Solr resource loader.
     * @throws IOException in case of I/O failure (e.g. reading the conf file)
     */
    public void inform(final ResourceLoader loader) throws IOException {
        this.configuration = configuration(loader);
        this.variantsMap = configuration.entrySet()
                .stream()
                .flatMap(entry -> unitVariants(entry).stream().map(variant -> newEntry(variant, entry.getKey())))
                .collect(toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    @Override
    public QParser createParser(final String qstr, final SolrParams localParams, final SolrParams params, final SolrQueryRequest req) {
        if (qstr == null) {
            return null;
        }

        final StringBuilder query = new StringBuilder(" ").append(qstr.toLowerCase()).append(" ");
        final QueryBuilder builder = queryBuilder(query);

        variantsMap
                .entrySet()
                .forEach(entry -> {
                    for (final Integer indexOfUnit : indexesOf(query, entry.getKey())) {
                        final int indexOfAmount = previousWhitespaceIndex(query, indexOfUnit);
                        if (indexOfAmount != -1) {
                            final int amount = Integer.parseInt(query.substring(indexOfAmount, indexOfUnit).trim());
                            builder.newQuantityDetected(
                                    QuantityOccurrence.createNew(
                                            amount,
                                            entry.getKey(),
                                            entry.getValue(),
                                            indexOfUnit,
                                            indexOfAmount));
                        }
                    }
                });

        final String product = builder.product();
        debug(qstr, " => ", product);

        return builder.qparserPlugin().createParser(product, localParams, params, req);
    }

    abstract QueryBuilder queryBuilder(final StringBuilder query);

    int previousWhitespaceIndex(final StringBuilder q, final int unitIndex) {
        if (unitIndex == 0) {
            return -1;
        }

        boolean spaceBetweenUnitsAndMeasureMet = false;
        boolean atLeastOneDigitHasBeenMet = false;
        int i = unitIndex - 1;
        for (; i >= 0; i--) {
            if (Character.isLetter(q.charAt(i))) {
                return -1;
            }

            if (Character.isDigit(q.charAt(i))) {
                atLeastOneDigitHasBeenMet = true;
            }

            if (i == 0) {
                if (atLeastOneDigitHasBeenMet) {
                    return i;
                } else {
                    return -1;
                }
            }

            if (Character.isWhitespace(q.charAt(i))) {
                if (spaceBetweenUnitsAndMeasureMet) {
                    return i;
                } else {
                    spaceBetweenUnitsAndMeasureMet = true;
                }
            }
        }
        return i;
    }

    /**
     * Returns a list containing the start indexes of all the occurrences of a variant within the given query.
     *
     * @param query the input query string.
     * @param variant the variant.
     * @return a list containing the all start indexes of the given variant within the query.
     */
    final List<Integer> indexesOf(final StringBuilder query, final String variant) {
        final List<Integer> indexes = new ArrayList<>();
        int indexOf = -1;
        final String searchKey = variant + " ";
        while ( (indexOf = query.indexOf(searchKey, indexOf + 1)) != -1) {
            indexes.add(indexOf);
        }
        return indexes;
    }

    /**
     * Returns a map representing the configuration associated with this component.
     *
     * @param loader the Solr resource loader.
     * @return a map representing the configuration associated with this component.
     * @throws IOException in case of I/O failure (e.g. reading the conf file)
     */
    private Map<String, Object> configuration(final ResourceLoader loader) throws IOException {
        try (final InputStream inputStream = loader.openResource("units.yml")) {
            final Yaml yaml = new Yaml();
            return (Map<String, Object>) yaml.load(inputStream);
        }
    }

    final Optional<Number> gap(final String fieldName) {
        final Map<String, Object> fieldConfiguration = (Map<String, Object>) configuration.get(fieldName);
        return fieldConfiguration != null
                ? Optional.ofNullable((Number)fieldConfiguration.get("gap"))
                : Optional.empty();
    }

    private List<String> unitVariants(final Map.Entry<String, Object> unitEntry) {
        return (List<String>)((Map<String, Object>)unitEntry.getValue()).getOrDefault("variants", Collections.emptyList());
    }

    private Map.Entry<String, String> newEntry(final String key, final String value) {
        return new SimpleEntry(key, value);
    }

    protected void debug(final String part1, final String part2, final String part3) {
        if (logger.isDebugEnabled()) {
            logger.debug(part1 + part2 + part3);
        }
    }
}