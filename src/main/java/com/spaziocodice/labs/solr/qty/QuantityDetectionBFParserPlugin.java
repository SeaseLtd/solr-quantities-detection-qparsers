package com.spaziocodice.labs.solr.qty;

import com.spaziocodice.labs.solr.qty.domain.EquivalenceTable;
import com.spaziocodice.labs.solr.qty.domain.QuantityOccurrence;
import com.spaziocodice.labs.solr.qty.domain.Unit;
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
    QueryBuilder queryBuilder(final StringBuilder query) {
        return new QueryBuilder() {
            final StringBuilder buffer = new StringBuilder();

            @Override
            public void newQuantityDetected(
                    final EquivalenceTable equivalenceTable,
                    final Unit unit,
                    final QuantityOccurrence detected) {

                unit.getVariantByName(detected.unit())
                        .ifPresent(variant -> {
                            final Number amount = equivalenceTable.equivalent(variant.refName(), detected.amount());
                            detected.fieldNames()
                                    .stream()
                                    .map(fieldName ->
                                            buffer
                                                .append("recip(abs(sub(")
                                                .append(fieldName)
                                                .append(", ")
                                                .append(amount)
                                                .append(")),1,1000,1000) "))
                                    .collect(joining(" ", " ", " "));
                        });
            }

            @Override
            public String product() {
                return buffer.length() > 0 ? buffer.toString().trim() : "1";
            }

            @Override
            public QParserPlugin qparserPlugin() {
                return qParser;
            }
        };
    }
}
