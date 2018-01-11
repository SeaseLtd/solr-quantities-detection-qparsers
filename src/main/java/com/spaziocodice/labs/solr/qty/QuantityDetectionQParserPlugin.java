package com.spaziocodice.labs.solr.qty;

import com.spaziocodice.labs.solr.qty.domain.EquivalenceTable;
import com.spaziocodice.labs.solr.qty.domain.QuantityOccurrence;
import com.spaziocodice.labs.solr.qty.domain.Unit;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.ExtendedDismaxQParserPlugin;
import org.apache.solr.search.QParserPlugin;

import java.util.Set;
import java.util.TreeSet;

/**
 * A {@link QParserPlugin} which detects and removes all quantities from the input query string.
 *
 * @author agazzarini
 * @since 1.0
 */
public class QuantityDetectionQParserPlugin extends QuantityDetector {
    private ExtendedDismaxQParserPlugin qParser;

    @Override
    public void init(final NamedList args) {
        super.init(args);
        this.qParser = new ExtendedDismaxQParserPlugin();
    }

    @Override
    QueryBuilder queryBuilder(final StringBuilder query) {
        return new QueryBuilder() {
            final Set<QuantityOccurrence> occurrences = new TreeSet<>();
            final StringBuilder buffer = new StringBuilder(query);

            @Override
            public void newQuantityDetected(
                    final EquivalenceTable equivalenceTable,
                    final Unit unit,
                    final QuantityOccurrence occurrence) {
                occurrences.add(occurrence);
            }

            @Override
            public void newHeuristicQuantityDetected(
                    final EquivalenceTable equivalenceTable,
                    final Unit unit,
                    final QuantityOccurrence occurrence) {
                // Do nothing here
            }

            @Override
            public String product() {
                occurrences.forEach(occurrence ->
                        buffer.delete(occurrence.indexOfAmount(), occurrence.indexOfUnit() + occurrence.unit().length()));
                final String result = buffer.toString().trim();
                return result.isEmpty() ? "*:*" : result;
            }

            @Override
            public QParserPlugin qparserPlugin() {
                return qParser;
            }
        };
    }
}