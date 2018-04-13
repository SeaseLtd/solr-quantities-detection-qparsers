package io.sease.solr.qty;

import io.sease.solr.qty.domain.EquivalenceTable;
import io.sease.solr.qty.domain.QuantityOccurrence;
import io.sease.solr.qty.domain.Unit;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.FunctionQParserPlugin;
import org.apache.solr.search.QParserPlugin;

import static java.util.stream.Collectors.joining;

/**
 * A {@link QParserPlugin} which produces a boost function according with the detected quantities within a query string.
 *
 * @author agazzarini
 * @since 1.0
 */
public class QuantityDetectionBFParserPlugin extends QuantityDetector {
    private FunctionQParserPlugin qParser;

    @Override
    public void init(final NamedList args) {
        super.init(args);
        this.qParser = new FunctionQParserPlugin();
    }

    @Override
    public QParserPlugin qparserPlugin() {
        return qParser;
    }

    @Override
    QueryBuilder queryBuilder(final StringBuilder query, final SolrParams params) {
        return new QueryBuilder() {
            final StringBuilder buffer = new StringBuilder();

            @Override
            public void newQuantityDetected(
                    final EquivalenceTable equivalenceTable,
                    final Unit unit,
                    final QuantityOccurrence occurrence) {
                onQuantityDetected(equivalenceTable, unit, occurrence);
            }

            @Override
            public void newHeuristicQuantityDetected(
                    final EquivalenceTable equivalenceTable,
                    final Unit unit,
                    final QuantityOccurrence occurrence) {
                onQuantityDetected(equivalenceTable, unit, occurrence);
            }

            private void onQuantityDetected(
                    final EquivalenceTable equivalenceTable,
                    final Unit unit,
                    final QuantityOccurrence detected) {

                unit.getVariantByName(detected.unit())
                        .ifPresent(variant -> {
                            final Number amount = equivalenceTable.equivalent(variant.refName(), detected.amount());
                            unit.fieldNames()
                                    .stream()
                                    .map(fieldName ->
                                            buffer
                                                .append("recip(abs(sub(")
                                                .append(fieldName)
                                                .append(", ")
                                                .append(amount)
                                                .append(")),")
                                                .append(params.getInt("m", 1))
                                                .append(",")
                                                .append(params.getInt("a", 1000))
                                                .append(",")
                                                .append(params.getInt("b", 1000))
                                                .append(") "))
                                    .collect(joining(" ", " ", " "));
                        });
            }

            @Override
            public String product() {
                return buffer.length() > 0 ? buffer.toString().trim() : "1";
            }
        };
    }
}
