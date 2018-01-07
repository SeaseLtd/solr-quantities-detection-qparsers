package com.spaziocodice.labs.solr.qty;

import com.spaziocodice.labs.solr.qty.domain.EquivalenceTable;
import com.spaziocodice.labs.solr.qty.domain.QuantityOccurrence;
import com.spaziocodice.labs.solr.qty.domain.Unit;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.LuceneQParserPlugin;
import org.apache.solr.search.QParserPlugin;

import static com.spaziocodice.labs.solr.qty.F.narrow;
import static com.spaziocodice.labs.solr.qty.F.narrowAsComparable;
import static com.spaziocodice.labs.solr.qty.domain.QuantityOccurrence.newQuantityOccurrence;
import static java.util.stream.Collectors.joining;

/**
 * A {@link QParserPlugin} which produces a boost query according with the detected quantities within a query string.
 * The generated query contains (for each detected quantity) a literal query (e.g. capacity:100) and an optional
 * range query (e.g. capacity:[90 TO 110]) depending on the configured gap.
 *
 * @author agazzarini
 * @since 1.0
 */
public class QuantityDetectionBQParserPlugin extends QuantityDetector {
    private LuceneQParserPlugin qParser;

    @Override
    public void init(final NamedList args) {
        super.init(args);
        this.qParser = new LuceneQParserPlugin();
    }

    @Override
    QueryBuilder queryBuilder(final StringBuilder query) {
        return new QueryBuilder() {
            final StringBuilder buffer = new StringBuilder();

            @Override
            public void newQuantityDetected(
                    final EquivalenceTable equivalenceTable,
                    final Unit unit,
                    final QuantityOccurrence detected) {
                unit.getVariantByName(detected.unit()).ifPresent(
                                variant -> {
                                    final QuantityOccurrence occurrence =
                                            newQuantityOccurrence(
                                                    equivalenceTable.equivalent(variant.refName(), detected.amount()),
                                                    unit.name(),
                                                    unit.fieldNames());
                                    addLiteralQuery(unit, buffer, occurrence);
                                    unit.fieldNames().stream()
                                            .map(unit::gap)
                                            .filter(gap -> gap.y.isPresent())
                                            .forEach(gap -> addRangeQuery(gap.x, gap.y.get(), buffer, occurrence));
                                });
            }

            @Override
            public String product() {
                return buffer.length() > 0 ? buffer.toString() : "*:*";
            }

            @Override
            public QParserPlugin qparserPlugin() {
                return qParser;
            }
        };
    }

    /**
     * Adds a new boolean, literal filter to the result of this builder.
     *
     * @param unit the unit associated with the detected quantity occurrence.
     * @param builder the query buffer.
     * @param occurrence the quantity instance occurrence.
     */
    private void addLiteralQuery(final Unit unit, final StringBuilder builder, final QuantityOccurrence occurrence) {
        builder.append(
                unit.fieldNames().stream()
                    .map(fieldName -> {
                        final StringBuilder bq = new StringBuilder()
                            .append(fieldName)
                            .append(":")
                            .append(occurrence.amount());
                        unit.boost(fieldName).ifPresent(boost -> bq.append("^").append(boost));
                        return bq;
                    }).collect(joining( " ", "", " ")));
    }

    /**
     * Adds a new boolean, range filter to the result of this builder.
     *
     * @param gap the gap associated with the detected quantity occurrence.
     * @param builder the query buffer.
     * @param occurrence the quantity instance occurrence.
     * @return the same query buffer with the new filter definition.
     */
    private StringBuilder addRangeQuery(
            final String fieldName,
            final Unit.Gap gap,
            final StringBuilder builder,
            final QuantityOccurrence occurrence) {

        final Comparable detectedAmount = narrowAsComparable(occurrence.amount());

        Number leftBound;
        Number rightBound;

        switch (gap.mode()) {
            case MAX:
                leftBound =
                        gap.value() != null
                            ? detectedAmount.compareTo(narrowAsComparable(gap.value())) >= 0
                                ? narrow(occurrence.amount().floatValue() - gap.value().floatValue())
                                : 0
                            : 0;
                rightBound = narrow(occurrence.amount());
                break;
            case MIN:
                leftBound = narrow(occurrence.amount());
                rightBound =
                        gap.value() != null
                                ? narrow(occurrence.amount().floatValue() + gap.value().floatValue())
                                : -1;
                break;
            default:
                final float distance = gap.value().floatValue();
                leftBound = occurrence.amount().floatValue() >= distance
                                ? narrow(occurrence.amount().floatValue() - distance)
                                : 0;
                rightBound = narrow(occurrence.amount().floatValue() + distance);
                break;
        }

        return builder
                .append(fieldName)
                .append(":[")
                .append(leftBound)
                .append(" TO ")
                .append(rightBound.intValue() != -1 ? rightBound : "*")
                .append("] ");
    }
}