package com.spaziocodice.labs.solr.qty;

import com.spaziocodice.labs.solr.qty.cfg.Unit;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.FunctionQParserPlugin;
import org.apache.solr.search.QParserPlugin;

/**
 * A {@link QParserPlugin} which produces a boost function according with the detected quantities within a query string.
 *
 * @author agazzarini
 * @since 1.0
 */
public class QuantityDetectionBFParserPlugin extends QuantityDetector {
    private FunctionQParserPlugin qParser;

    @Override
    public void init(NamedList args) {
        super.init(args);
        this.qParser = new FunctionQParserPlugin();
    }

    @Override
    QueryBuilder queryBuilder(final StringBuilder query) {
        return new QueryBuilder() {
            final StringBuilder buffer = new StringBuilder();

            @Override
            public void newQuantityDetected(final Unit unit, final QuantityOccurrence occurrence) {
                buffer
                    .append("recip(abs(sub(")
                    .append(occurrence.fieldName)
                    .append(", ")
                    .append(occurrence.amount)
                    .append(")),1,1000,1000) ");
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
