package io.sease.solr.qty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Assumptions test case: detected orphan quantities aren't removed from the main query.
 *
 * @author agazzarini
 * @since 1.0
 */
public class AssumptionsWithoutRemovalTestCase {
    private QuantityDetectionBQParserPlugin bq;
    private QuantityDetectionQParserPlugin q;
    private QuantityDetectionBFParserPlugin bf;
    private final SolrParams params = new ModifiableSolrParams();

    @Before
    public void setUp() throws Exception {
        final JsonNode configuration = new ObjectMapper().readTree(new File("src/test/resources/assumptions.json"));
        q =new QuantityDetectionQParserPlugin() {
            @Override
            JsonNode configuration(final ResourceLoader loader) {
                return configuration;
            }
        };

        bq = new QuantityDetectionBQParserPlugin() {
            @Override
            JsonNode configuration(final ResourceLoader loader) {
                return configuration;
            }
        };

        bf = new QuantityDetectionBFParserPlugin() {
            @Override
            JsonNode configuration(final ResourceLoader loader) {
                return configuration;
            }
        };

        q.init(mock(NamedList.class));
        q.inform(mock(ResourceLoader.class));

        bq.init(mock(NamedList.class));
        bq.inform(mock(ResourceLoader.class));

        bf.init(mock(NamedList.class));
        bf.inform(mock(ResourceLoader.class));
    }

    @Test
    public void oneQuantity() {
        final Map<String, List<String>> data = new HashMap<>();
        data.put("fridge 0.30", asList("fridge 0.30","capacity:0.3", "recip(abs(sub(capacity, 0.3)),1,1000,1000)"));
        data.put("fridge 0.50", asList("fridge 0.50","capacity:0.5", "recip(abs(sub(capacity, 0.5)),1,1000,1000)"));
        data.put("bottle 250", asList("bottle 250","capacity:2.5", "recip(abs(sub(capacity, 2.5)),1,1000,1000)"));
        data.put("fridge 1898", asList("fridge 1898","height:1898", "recip(abs(sub(height, 1898)),1,1000,1000)"));

        data.forEach((query, expected) -> {
            assertEquals(
                    query,
                    expected.get(0),
                    q.buildQuery(query, params));

            assertEquals(
                    query,
                    expected.get(1),
                    bq.buildQuery(query, params));

            assertEquals(
                    query,
                    expected.get(2),
                    bf.buildQuery(query, params));
        });
    }

    @Test
    public void defaultUnit() {
        final Map<String, List<String>> data = new HashMap<>();
        data.put(" fridge 4593", asList("fridge 4593","voltage:4593", "recip(abs(sub(voltage, 4593)),1,1000,1000)"));

        data.forEach((query, expected) -> {
            assertEquals(
                    query,
                    expected.get(0),
                    q.buildQuery(query, params));

            assertEquals(
                    query,
                    expected.get(1),
                    bq.buildQuery(query, params));

            assertEquals(
                    query,
                    expected.get(2),
                    bf.buildQuery(query, params));
        });
    }

    @Test
    public void noDetectedAmount() {
        final Map<String, List<String>> data = new HashMap<>();
        data.put(" fridge model no. 1317abx ", asList("fridge model no. 1317abx","*:*", "1"));

        data.forEach((query, expected) -> {
            assertEquals(
                    query,
                    expected.get(0),
                    q.buildQuery(query, params));

            assertEquals(
                    query,
                    expected.get(1),
                    bq.buildQuery(query, params));

            assertEquals(
                    query,
                    expected.get(2),
                    bf.buildQuery(query, params));
        });
    }
}