package com.spaziocodice.labs.solr.qty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spaziocodice.labs.solr.qty.cfg.Unit;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.*;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

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
    /**
     *
     */
    interface QueryBuilder {
        /**
         * A new quantity (i.e. amount + unit) has been detected.
         * When this event occurs, the builder is notified through this callback
         * with a {@link QuantityOccurrence} instance which contains all information
         * (i.e. amount, unit, offsets) about the detected quantity.
         *
         * @param occurrence the occurrence encapsulating the quantity detection.
         */
        void newQuantityDetected(final QuantityOccurrence occurrence);

        /**
         * Returns the built query, that is, the product of this builder.
         *
         * @return the built query, that is, the product of this builder.
         */
        String product();

        /**
         * Returns the internal {@link QParserPlugin} that will be used for
         * parsing the built query.
         *
         * @return the internal {@link QParserPlugin} that will be used for
         * parsing the built query.
         */
        QParserPlugin qparserPlugin();
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, String> variantsMap;
    private List<Unit> units;

    /**
     * Completes the initialization of this component by loading the provided configuration.
     *
     * @param loader the Solr resource loader.
     * @throws IOException in case of I/O failure (e.g. reading the conf file)
     */
    public void inform(final ResourceLoader loader) throws IOException {
        units = units(configuration(loader));
        variantsMap = units.stream()
                .flatMap(unit -> {
                    final Set<String> forms = new HashSet<>();
                    forms.add(unit.name());

                    unit.variants()
                            .forEach(variant -> {
                                forms.add(variant.refName());
                                forms.addAll(variant.forms());
                            });
                    return forms.stream().map(form -> new SimpleEntry<>(form, unit.fieldName()));})
                .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    @Override
    public final QParser createParser(final String qstr, final SolrParams localParams, final SolrParams params, final SolrQueryRequest req) {
        if (qstr == null) {
            return null;
        }

        final StringBuilder query = new StringBuilder(" ").append(qstr.toLowerCase()).append(" ");
        final QueryBuilder builder = queryBuilder(query);

        final String product = buildQuery(builder, query);
        debug(qstr, product);

        return builder.qparserPlugin().createParser(product, localParams, params, req);
    }

    String buildQuery(final QueryBuilder builder, final StringBuilder query) {
        variantsMap
              .forEach((variant, fieldName) -> {
                  for (final Integer unitOffset : indexesOf(query, variant)) {
                      final int amountOffset = startIndexOfAmount(query, unitOffset);
                      // TODO: use Optional instead
                      if (amountOffset != -1) {
                          final int amount = Integer.parseInt(query.substring(amountOffset, unitOffset).trim());
                          builder.newQuantityDetected(
                                  QuantityOccurrence.newOccurrence(
                                          amount,
                                          variant,
                                          fieldName,
                                          unitOffset,
                                          amountOffset));
                      }
                  }
              });
        return builder.product().trim();
    }

    /**
     * Returns the query builder instance associated with this detector.
     *
     * @param query the input query (as a {@link StringBuilder}.
     * @return the query builder instance associated with this detector.
     */
    abstract QueryBuilder queryBuilder(StringBuilder query);

    /**
     * Internal method for detecting the start offset of the (potential) detected quantity.
     *
     * @param q the input query buffer.
     * @param unitIndex the start offset of the unit.
     * @return the start offset of the (potential) detected quantity, -1 in case the detection is not a quantity.
     */
    int startIndexOfAmount(final StringBuilder q, final int unitIndex) {
        if (unitIndex <= 0) {
            return -1;
        }

        boolean atLeastOneDigitHasBeenMet = false;
        int i = unitIndex - 1;
        for (; i >= 0; i--) {
            final char ch = q.charAt(i);
            if (Character.isLetter(ch)) {
                return -1;
            }

            if (Character.isDigit(ch)) {
                atLeastOneDigitHasBeenMet = true;
            }

            if (Character.isWhitespace(ch) && atLeastOneDigitHasBeenMet) {
                    return i + 1;
            }

            if (i == 0 && atLeastOneDigitHasBeenMet) {
                return i;
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
        if (query.length() <= 2) {
            return Collections.emptyList();
        }

        if (query.charAt(0) != ' ') {
            query.insert(0, ' ');
        }

        if (query.charAt(query.length() - 1) != ' ') {
            query.append(" ");
        }

        final List<Integer> indexes = new ArrayList<>();
        int indexOf = -1;
        while ( (indexOf = query.indexOf(variant, indexOf + 1)) != -1) {
            if ((indexOf == 0 || !Character.isLetter(query.charAt(indexOf - 1)))
                    && !Character.isLetterOrDigit(query.charAt(indexOf + variant.length()))) {
                indexes.add(indexOf);
            }
        }
        return indexes;
    }

    /**
     * Returns the gap associated with the given field name.
     *
     * @param fieldName the field name.
     * @return the gap associated with the given field name.
     */
    final Optional<Unit.Gap> gap(final String fieldName) {
        final Optional<Unit> unit = unit(fieldName);
        return unit.isPresent() ? unit.get().gap() : Optional.empty();
    }

    private Optional<Unit> unit(final String fieldName) {
        return units.stream()
                .filter(unit -> unit.fieldName().equals(fieldName))
                .findFirst();
    }

    private List<Unit> units(final JsonNode configuration) {
        return stream(configuration.get("units").spliterator(), false)
                .map(unitNode -> {
                    final String fieldName =  unitNode.fieldNames().next();
                    final JsonNode unitCfg = unitNode.get(fieldName);

                    final String unitName = unitCfg.get("unit").asText();

                    final Unit unit = new Unit(
                            fieldName,
                            unitName, unitCfg.hasNonNull("boost") ? unitCfg.get("boost").floatValue() : 1f);

                    ofNullable(unitCfg.get("gap"))
                            .ifPresent(gap ->
                                unit.setGap(
                                        gap.get("value").floatValue(),
                                        gap.get("mode").asText("PIVOT")));

                    ofNullable(unitCfg.get("variants"))
                            .ifPresent(variants ->
                                variants.fieldNames()
                                        .forEachRemaining(mainFormName ->
                                            unit.addVariant(
                                                    mainFormName,
                                                    stream(variants.get(mainFormName).spliterator(), false)
                                                            .map(JsonNode::asText)
                                                            .collect(toList()))));
                    return unit;
                }).collect(toList());
    }

    /**
     * Logs out a (2-parts) message in DEBUG level.
     *
     * @param part1 the first message part.
     * @param part2 the second message part.
     */
    private void debug(final String part1, final String part2) {
        if (logger.isDebugEnabled()) {
            logger.debug(part1 + " => " + part2);
        }
    }

    /**
     * Loads the configuration associated with this component.
     *
     * @param loader the Solr resource loader.
     * @return the configuration associated with this component.
     * @throws IOException in case of I/O failure (e.g. configuration file not found).
     */
    JsonNode configuration(final ResourceLoader loader) throws IOException {
        return new ObjectMapper().readTree(loader.openResource("units.json"));
    }

}