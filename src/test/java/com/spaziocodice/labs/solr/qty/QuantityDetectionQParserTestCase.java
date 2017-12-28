package com.spaziocodice.labs.solr.qty;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.ExtendedDismaxQParserPlugin;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.spaziocodice.labs.TestData.LT;
import static java.util.Arrays.stream;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Quantity Detection "Q" Parser test case.
 *
 * @author agazzarini
 * @since 1.0
 */
public class QuantityDetectionQParserTestCase {
    private QuantityDetectionQParserPlugin cut;

    @Before
    public void setUp() throws Exception {
        cut = new QuantityDetectionQParserPlugin() {
            @Override
            Map<String, Object> configuration(final ResourceLoader loader) {
                final Map<String, Object> configuration = new HashMap<>();
                final Map<String, List<String>> variants = new HashMap<>();
                variants.put("variants", Collections.singletonList(LT));
                configuration.put("capacity", variants);
                return configuration;
            }
        };

        cut.init(mock(NamedList.class));
        cut.inform(mock(ResourceLoader.class));
    }

    /**
     * The detection qparser must use the edismax qparser, internally.
     */
    @Test
    public void queryParser() {
        assertSame(ExtendedDismaxQParserPlugin.class, cut.queryBuilder(new StringBuilder()).qparserPlugin().getClass());
    }

    /**
     * If no quantities are detected on a given query, then the query string is
     * left untouched.
     */
    @Test
    public void noQuantities() {
        final String [] noQuantityQueries = {
                "There's no quantity here",
                "Even if lt is a configured unit, we have no amount, so no quantity.",
                "ABCDlt or ABCD lt isn't a quantity."
        };

        stream(noQuantityQueries)
                .map(StringBuilder::new)
                .forEach(query -> assertEquals(query.toString(), cut.buildQuery(cut.queryBuilder(query), query)));
    }

    @Test
    public void oneQuantity() {
        final Map<String, String> data = new HashMap<>();
        data.put("There a 100lt quantity here", "There a  quantity here");
        data.put("There a 10230 lt quantity here", "There a  quantity here");
        data.put("100lt", "*:*");
        data.put("100 lt", "*:*");
        data.put("  100 lt ", "*:*");

        data.forEach((input, expected) -> {
                    final StringBuilder query = new StringBuilder(input);
                    assertEquals(expected, cut.buildQuery(cut.queryBuilder(query), query));
                });
    }

    @Test
    public void multipleQuantities() {
        final Map<String, String> data = new HashMap<>();
        data.put("There a 100lt quantity here, and another 50 lt here", "There a  quantity here, and another  here");
        data.put("129 lt. There a 10230 lt, another 553lt here, and another 293 lt here. Other 992 lt", ". There a , another  here, and another  here. Other");
        data.put("100lt 234lt 888 lt 992 lt", "*:*");

        data.forEach((input, expected) -> {
            final StringBuilder query = new StringBuilder(input);
            assertEquals(expected, cut.buildQuery(cut.queryBuilder(query), query));
        });
    }
}
