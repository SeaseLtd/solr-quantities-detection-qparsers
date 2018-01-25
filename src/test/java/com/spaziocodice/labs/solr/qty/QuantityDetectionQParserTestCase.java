package com.spaziocodice.labs.solr.qty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.ExtendedDismaxQParserPlugin;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    private final SolrParams params = new ModifiableSolrParams();

    @Before
    public void setUp() throws Exception {
        cut = new QuantityDetectionQParserPlugin() {
            @Override
            JsonNode configuration(final ResourceLoader loader) throws IOException {
                return new ObjectMapper().readTree(new File("src/test/resources/q_units.json"));
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
        assertSame(ExtendedDismaxQParserPlugin.class, cut.qparserPlugin().getClass());
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
                .forEach(q -> assertEquals(q.toLowerCase(), cut.buildQuery(q, params)));
    }

    @Test
    public void oneQuantity() {
        final Map<String, String> data = new HashMap<>();
        data.put("There a 100lt quantity here", "there a  quantity here");
        data.put("There a 10230 lt quantity here", "there a  quantity here");
        data.put("100lt", "*:*");
        data.put("100 lt", "*:*");
        data.put("  100 lt ", "*:*");

        data.forEach((input, expected) -> assertEquals( input, expected, cut.buildQuery(input, params)));
    }

    @Test
    public void multipleQuantities() {
        final Map<String, String> data = new HashMap<>();
        data.put("There a 100lt quantity here, and another 50 lt here", "there a  quantity here, and another  here");
        data.put("129 lt. There a 10230 lt, another 553lt here, and another 293 lt here. Other 992 lt", ". there a , another  here, and another  here. other");
        data.put("100lt 234lt 888 lt 992 lt", "*:*");

        data.forEach((input, expected) -> assertEquals(input, expected, cut.buildQuery(input, params)));
    }
}