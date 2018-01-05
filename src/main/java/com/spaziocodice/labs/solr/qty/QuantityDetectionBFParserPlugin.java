package com.spaziocodice.labs.solr.qty;

import com.spaziocodice.labs.solr.qty.domain.EquivalenceTable;
import com.spaziocodice.labs.solr.qty.domain.QuantityOccurrence;
import com.spaziocodice.labs.solr.qty.domain.Unit;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.FunctionQParserPlugin;
import org.apache.solr.search.QParserPlugin;

import static com.spaziocodice.labs.solr.qty.F.narrow;
import static com.spaziocodice.labs.solr.qty.domain.QuantityOccurrence.newQuantityOccurrence;

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

                unit.getVariantByName(
                        detected.unit()).ifPresent(
                        variant ->
                            buffer
                                .append("recip(abs(sub(")
                                .append(detected.fieldName())
                                .append(", ")
                                .append(equivalenceTable.equivalent(variant.refName(), detected.amount()))
                                .append(")),1,1000,1000) "));
            }

            @Override
            public String product() {
                return buffer.length() > 0 ? buffer.toString() : "1";
            }

            @Override
            public QParserPlugin qparserPlugin() {
                return qParser;
            }
        };
    }
}
