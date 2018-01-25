package com.spaziocodice.labs.solr.qty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.FunctionQParserPlugin;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Quantity Detection "BF" Parser test case.
 *
 * @author agazzarini
 * @since 1.0
 */
public class QuantityDetectionBFParserTestCase {
    private QuantityDetectionBFParserPlugin cut;
    private SolrParams params;

    @Before
    public void setUp() throws Exception {
        cut = new QuantityDetectionBFParserPlugin() {
            @Override
            JsonNode configuration(final ResourceLoader loader) throws IOException {
                return new ObjectMapper().readTree(new File("src/test/resources/bf_units.json"));
            }
        };

        params = new ModifiableSolrParams();

        cut.init(mock(NamedList.class));
        cut.inform(mock(ResourceLoader.class));
    }

    /**
     * The detection qparser must use the function qparser, internally.
     */
    @Test
    public void queryParser() {
        assertSame(FunctionQParserPlugin.class, cut.qparserPlugin().getClass());
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

        stream(noQuantityQueries).forEach(query -> assertEquals("1", cut.buildQuery(query, params)));
    }

    @Test
    public void oneQuantityWithoutGap() {
        asList(
            "There a 100cm quantity here",
            "There a 100 cm quantity here",
            "100cm",
            "100 cm",
            "  100 cm ")
            .forEach(
                    query ->
                        assertEquals(
                                query,
                                "recip(abs(sub(height, 100)),1,1000,1000)",
                                cut.buildQuery(query, params)));
    }

    @Test
    public void yetOneQuantity() {
        asList(
            "There a 100lt quantity here",
            "There a 100 lt quantity here",
            "100lt",
            "100 lt",
            "  100 lt ")
            .forEach(
                    query ->
                        assertEquals(
                                "recip(abs(sub(capacity, 100)),1,1000,1000)",
                                cut.buildQuery(query, params)));
    }


    @Test
    public void multipleQuantitiesOfOneKind() {
        final Map<String, String> data = new HashMap<>();
        data.put("There a 100lt quantity here, and another 50 lt here", "recip(abs(sub(capacity, 100)),1,1000,1000) recip(abs(sub(capacity, 50)),1,1000,1000)");
        data.put("129 lt. There a 10230 lt, another 553lt here, and another 293 lt here. Other 992 lt", "recip(abs(sub(capacity, 129)),1,1000,1000) recip(abs(sub(capacity, 10230)),1,1000,1000) recip(abs(sub(capacity, 553)),1,1000,1000) recip(abs(sub(capacity, 293)),1,1000,1000) recip(abs(sub(capacity, 992)),1,1000,1000)");
        data.put("100lt 234lt 888 lt 992 lt", "recip(abs(sub(capacity, 100)),1,1000,1000) recip(abs(sub(capacity, 234)),1,1000,1000) recip(abs(sub(capacity, 888)),1,1000,1000) recip(abs(sub(capacity, 992)),1,1000,1000)");

        data.forEach((input, expected) ->  assertEquals(input, expected, cut.buildQuery(input, params)));
    }

    @Test
    public void multipleQuantitiesMixed() {
        final Map<String, String> data = new HashMap<>();
        data.put("There a 100lt quantity here, and another 50 cm here", "recip(abs(sub(capacity, 100)),1,1000,1000) recip(abs(sub(height, 50)),1,1000,1000)");
        data.put("129 lt. There a 10230 cm, another 553cm here, and another 293 lt here. Other 992 cm", "recip(abs(sub(capacity, 129)),1,1000,1000) recip(abs(sub(capacity, 293)),1,1000,1000) recip(abs(sub(height, 10230)),1,1000,1000) recip(abs(sub(height, 553)),1,1000,1000) recip(abs(sub(height, 992)),1,1000,1000)");
        data.put("100lt 234lt 888 cm 992 lt", "recip(abs(sub(capacity, 100)),1,1000,1000) recip(abs(sub(capacity, 234)),1,1000,1000) recip(abs(sub(capacity, 992)),1,1000,1000) recip(abs(sub(height, 888)),1,1000,1000)");

        data.forEach((input, expected) -> assertEquals(input, expected, cut.buildQuery(input, params)));
    }
}