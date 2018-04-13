package io.sease.solr.qty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.LuceneQParserPlugin;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Quantity Detection "BQ" Parser test case.
 *
 * @author agazzarini
 * @since 1.0
 */
public class QuantityDetectionBQParserTestCase {
    private QuantityDetectionBQParserPlugin cut;
    private final SolrParams params = new ModifiableSolrParams();

    @Before
    public void setUp() throws Exception {
        cut = new QuantityDetectionBQParserPlugin() {
            @Override
            JsonNode configuration(final ResourceLoader loader) throws IOException {
                return new ObjectMapper().readTree(new File("src/test/resources/bq_units.json"));
            }
        };

        cut.init(mock(NamedList.class));
        cut.inform(mock(ResourceLoader.class));
    }

    /**
     * The detection qparser must use the lucene qparser, internally.
     */
    @Test
    public void queryParser() {
        assertSame(LuceneQParserPlugin.class, cut.qparserPlugin().getClass());
    }

    /**
     * If no quantities are detected on a given query, then the query string is
     * left untouched.
     */
    @Test
    public void noQuantities() {
        final String [] noQuantityQueries = {
                "There's no quantity here",
                "Even if lt is a configured unit, we have no amount here, so no quantity.",
                "ABCDlt or ABCD lt isn't a quantity."
        };

        stream(noQuantityQueries).forEach(q -> assertEquals("", cut.buildQuery(q, params)));
    }

    @Test
    public void oneQuantityWithGap() {
        asList(
                "There's a 100cm quantity here",
                "There's a 100 cm quantity here",
                "100cm",
                "100 cm",
                "  100 cm ")
                .forEach(
                        q ->
                            assertEquals(
                                    q,
                                    "height:100 height:[90 TO 110]",
                                    cut.buildQuery(q, params)));
    }

    @Test
    public void quantityWithBoostAndNoGap() {
        asList(
            "There's a 100v quantity here",
            "There's a 100 v quantity here",
            "100volts",
            "100 v",
            "  100 volt ")
            .forEach(q -> assertEquals("voltage:100^1.3", cut.buildQuery(q, params)));
    }

    @Test
    public void boostAreAppliedOnlyToLiteralFilters() {
        asList(
            "There's a 100w quantity here",
            "There's a 100 watts quantity here",
            "100watts",
            "100 w",
            "  100 watt")
            .forEach(
                q ->
                    assertEquals(
                            q,
                            "wattage:100^1.3 wattage:[90 TO 110]",
                            cut.buildQuery(q, params)));
    }

    @Test
    public void multipleQuantitiesMixedWithBoosts() {
        final Map<String, String> data = new HashMap<>();
        data.put("There a 100lt quantity here, and another 50 w here", "capacity:100 wattage:50^1.3 wattage:[40 TO 60]");
        data.put("129 lt. There a 10230 cm, another 553watts here, and another 293 lt here. Other 992 watt", "capacity:129 capacity:293 height:10230 height:[10220 TO 10240] wattage:553^1.3 wattage:[543 TO 563] wattage:992^1.3 wattage:[982 TO 1002]");
        data.put("100lt 234lt 888 watt 992 lt", "capacity:100 capacity:234 capacity:992 wattage:888^1.3 wattage:[878 TO 898]");

        data.forEach((input, expected) -> assertEquals(input, expected, cut.buildQuery(input, params)));
    }

    @Test
    public void oneQuantityWithoutGap() {
        asList(
                "There's a 100lt quantity here",
                "There's a 100 lt quantity here",
                "100lt",
                "100 lt",
                "  100 lt ")
        .forEach(q -> assertEquals(q, "capacity:100", cut.buildQuery(q, params)));
    }

    @Test
    public void maxGapModeWithValue() {
        asList(
            "There a 100dollars quantity here",
            "There a 100 dollars quantity here",
            "100dollars",
            "100 dollars",
            "  100 dollars ")
            .forEach(
                    q ->
                        assertEquals(
                                q,
                                "price_max_with_value:100 price_max_with_value:[50 TO 100]",
                                cut.buildQuery(q, params)));
    }

    @Test
    public void maxGapModeWithoutValue() {
        asList(
            "There a 100cad quantity here",
            "There a 100 cad quantity here",
            "100cad",
            "100 cad",
            "  100 cad ")
            .forEach(
                    query ->
                            assertEquals(
                                    query,
                                    "price_max_without_value:100 price_max_without_value:[0 TO 100]",
                                    cut.buildQuery(query, params)));
    }

    @Test
    public void minGapModeWithValue() {
        asList(
            "There's a 100euro quantity here",
            "There's a 100 euro quantity here",
            "100euro",
            "100 euro",
            "  100 euro ")
            .forEach(
                    query ->
                        assertEquals(
                                query,
                                "price_min_with_value:100 price_min_with_value:[100 TO 110]",
                                cut.buildQuery(query, params)));
    }

    @Test
    public void minGapModeWithoutValue() {
        asList(
            "There's a 100francs quantity here",
            "There's a 100 francs quantity here",
            "100francs",
            "100 francs",
            "  100 francs ")
            .forEach(
                    query ->
                            assertEquals(
                                    query,
                                    "price_min_without_value:100 price_min_without_value:[100 TO *]",
                                    cut.buildQuery(query, params)));
    }

    @Test
    public void multipleQuantitiesWithoutGap() {
        final Map<String, String> data = new HashMap<>();
        data.put("There's a 100lt quantity here, and another 50 lt here", "capacity:100 capacity:50");
        data.put("129 lt. There a 10230 lt, another 553lt here, and another 293 lt here. Other 992 lt", "capacity:129 capacity:10230 capacity:553 capacity:293 capacity:992");
        data.put("100lt 234lt 888 lt 992 lt", "capacity:100 capacity:234 capacity:888 capacity:992");

        data.forEach((input, expected) -> assertEquals(input, expected, cut.buildQuery(input, params)));
    }

    @Test
    public void multipleQuantitiesMixed() {
        final Map<String, String> data = new HashMap<>();
        data.put("There's a 100lt quantity here, and another 50 cm here", "capacity:100 height:50 height:[40 TO 60]");
        data.put("129 lt. There a 10230 cm, another 553cm here, and another 293 lt here. Other 992 cm", "capacity:129 capacity:293 height:10230 height:[10220 TO 10240] height:553 height:[543 TO 563] height:992 height:[982 TO 1002]");
        data.put("100lt 234lt 888 cm 992 lt", "capacity:100 capacity:234 capacity:992 height:888 height:[878 TO 898]");

        data.forEach((input, expected) -> assertEquals(input, expected, cut.buildQuery(input, params)));
    }
}