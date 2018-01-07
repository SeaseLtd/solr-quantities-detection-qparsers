package com.spaziocodice.labs.solr.qty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spaziocodice.labs.solr.qty.domain.EquivalenceTable;
import com.spaziocodice.labs.solr.qty.domain.QuantityOccurrence;
import com.spaziocodice.labs.solr.qty.domain.Unit;
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

import static com.spaziocodice.labs.solr.qty.domain.QuantityOccurrence.newQuantityOccurrence;
import static java.util.Collections.unmodifiableList;
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
    private Unit unit;

    /**
     * Query builder.
     *
     * @author agazzarini
     * @since 1.0
     */
    interface QueryBuilder {
        /**
         * A new quantity (i.e. amount + unit) has been detected.
         * When this event occurs, the builder is notified through this callback
         * with a {@link QuantityOccurrence} instance which contains all information
         * (i.e. amount, unit, offsets) about the detected quantity.
         *
         * @param equivalenceTable the equivalence table.
         * @param unit the unit associated with the detected quantity.
         * @param occurrence the occurrence encapsulating the quantity detection.
         */
        void newQuantityDetected(final EquivalenceTable equivalenceTable, final Unit unit, final QuantityOccurrence occurrence);

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

    private Map<String, List<String>> variantsMap;
    private List<Unit> units;

    private EquivalenceTable equivalenceTable;

    /**
     * Completes the initialization of this component by loading the provided configuration.
     *
     * @param loader the Solr resource loader.
     * @throws IOException in case of I/O failure (e.g. reading the conf file)
     */
    public void inform(final ResourceLoader loader) throws IOException {
        final JsonNode configuration = configuration(loader);

        units = unmodifiableList(units(configuration));
        variantsMap = units.stream()
                .flatMap(unit -> {
                    final Set<String> forms = new HashSet<>();
                    forms.add(unit.name());
                    unit.variants()
                            .forEach(variant -> {
                                forms.add(variant.refName());
                                forms.addAll(variant.forms());
                            });
                    return forms.stream().map(form -> new SimpleEntry<>(form, unit.fieldNames()));})
                .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue));
        equivalenceTable = new EquivalenceTable(equivalenceTable(configuration));
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

    /**
     * Executes the build query process.
     *
     * @param builder the query builder, which depends on the specific qparser.
     * @param query the input query (as a {@link StringBuilder}).
     * @return the built query, according with the rules of the query builder associated with the qparser.
     */
    String buildQuery(final QueryBuilder builder, final StringBuilder query) {
        variantsMap
          .forEach((variant, fieldNames) ->
              unit(fieldNames)
                  .ifPresent(unit ->
                      indexesOf(query, variant)
                          .forEach(unitOffset ->
                              startIndexOfAmount(query, unitOffset)
                                  .ifPresent(
                                    amountOffset ->
                                        builder.newQuantityDetected(
                                                equivalenceTable,
                                                unit,
                                                newQuantityOccurrence(
                                                        query.substring(amountOffset, unitOffset).contains(".")
                                                            ? Double.parseDouble(query.substring(amountOffset, unitOffset).trim())
                                                            : Float.parseFloat(query.substring(amountOffset, unitOffset).trim()),
                                                        variant,
                                                        fieldNames,
                                                        unitOffset,
                                                        amountOffset))))));
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
    OptionalInt startIndexOfAmount(final StringBuilder q, final int unitIndex) {
        if (unitIndex <= 0) {
            return OptionalInt.empty();
        }

        boolean atLeastOneDigitHasBeenMet = false;
        int i = unitIndex - 1;
        for (; i >= 0; i--) {
            final char ch = q.charAt(i);
            if (Character.isLetter(ch)) {
                return OptionalInt.empty();
            }

            if (Character.isDigit(ch)) {
                atLeastOneDigitHasBeenMet = true;
            }

            if (Character.isWhitespace(ch) && atLeastOneDigitHasBeenMet) {
                    return OptionalInt.of(i + 1);
            }

            if (i == 0 && atLeastOneDigitHasBeenMet) {
                return OptionalInt.of(i);
            }
        }
        return i != -1 ? OptionalInt.of(i) : OptionalInt.empty();
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
     * Finds, in the configuration, the unit associated with the given field names.
     *
     * @param fieldNames the field name.
     * @return the unit associated with the given field name.
     */
    private Optional<Unit> unit(final List<String> fieldNames) {
        return units.stream()
                .filter(unit -> unit.fieldNames().equals(fieldNames))
                .findFirst();
    }

    /**
     * Finds, in the configuration, the unit associated with the given field name.
     *
     * @param fieldName the field name.
     * @return the unit associated with the given field name.
     */
    private Optional<Unit> unit(final String fieldName) {
        return units.stream()
                .filter(unit -> unit.fieldNames().contains(fieldName))
                .findFirst();
    }

    /**
     * Returns the equivalence table declared in the configuration.
     *
     * @param configuration this plugin configuration.
     * @return the equivalence table declared in the configuration.
     */
    private Map<String, Number> equivalenceTable(final JsonNode configuration) {
        final Map<String, Number> result = new HashMap<>();
        ofNullable(configuration.get("equivalence.table"))
            .ifPresent(table ->
               table.iterator().forEachRemaining(refUnit -> {
                    refUnit.fieldNames()
                            .forEachRemaining(name -> {
                                result.put(name, 1);
                                final JsonNode unitTable = refUnit.get(name);
                                unitTable
                                        .fieldNames()
                                        .forEachRemaining(unit -> result.put(unit, unitTable.get(unit).numberValue()));
                            });
               }));
        return result;
    }

    /**
     * Returns the units that have been configured within this instance configuration.
     *
     * @param configuration the component configuration.
     * @return the units that have been configured within this instance configuration.
     */
    private List<Unit> units(final JsonNode configuration) {
        return stream(configuration.get("units").spliterator(), false)
                .map(unitNode -> {
                    final String fieldNames =  unitNode.fieldNames().next();
                    final JsonNode unitCfg = unitNode.get(fieldNames);

                    final String unitName = unitCfg.get("unit").asText();

                    final Unit unit = new Unit(fieldNames, unitName);

                    ofNullable(unitCfg.get("boost"))
                        .ifPresent(boost -> {
                            if (boost.isObject()) {
                                ofNullable(boost.get("value")).ifPresent(value -> unit.setDefaultBoost(value.floatValue()));
                                unit.fieldNames()
                                        .forEach(fieldName ->
                                            ofNullable(boost.get(fieldName))
                                                    .ifPresent(boostNode ->
                                                        unit.addBoost(fieldName, boostNode.get("value").floatValue())));
                            } else {
                                unit.setDefaultBoost(boost.floatValue());
                            }
                        });

                    ofNullable(unitCfg.get("gap"))
                        .ifPresent(gap -> {
                            unit.setGap(
                                gap.hasNonNull("value") ? gap.get("value").floatValue() : null,
                                gap.get("mode").asText("PIVOT"));
                            
                            unit.fieldNames()
                                    .forEach(fieldName ->
                                        ofNullable(gap.get(fieldName))
                                                .ifPresent(override ->
                                                    unit.addGap(
                                                        fieldName,
                                                        override.hasNonNull("value") ? override.get("value").floatValue() : null,
                                                        override.get("mode").asText("PIVOT"))));
                        });

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