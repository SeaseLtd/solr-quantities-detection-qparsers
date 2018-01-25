package com.spaziocodice.labs.solr.qty;

import com.spaziocodice.labs.solr.qty.domain.EquivalenceTable;
import com.spaziocodice.labs.solr.qty.domain.QuantityOccurrence;
import com.spaziocodice.labs.solr.qty.domain.Unit;
import org.apache.solr.common.params.SolrParams;
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

    public final static String REMOVE_ORPHAN_AMOUNTS_PARAM_NAME = "removeOrphanAmounts";
    private boolean removeOrphanAmounts;

    @Override
    public void init(final NamedList args) {
        super.init(args);
        removeOrphanAmounts = SolrParams.toSolrParams(args).getBool(REMOVE_ORPHAN_AMOUNTS_PARAM_NAME, false);
        this.qParser = new ExtendedDismaxQParserPlugin();
    }

    @Override
    public QParserPlugin qparserPlugin() {
        return qParser;
    }

    @Override
    QueryBuilder queryBuilder(final StringBuilder query, final SolrParams params) {
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
                if (removeOrphanAmounts) {
                    occurrences.add(occurrence);
                }
            }

            @Override
            public String product() {
                occurrences.forEach(occurrence ->
                        buffer.delete(occurrence.startOffset(), occurrence.endOffset()));
                final String result = buffer.toString().trim();
                return result.isEmpty() ? "*:*" : result;
            }
        };
    }
}