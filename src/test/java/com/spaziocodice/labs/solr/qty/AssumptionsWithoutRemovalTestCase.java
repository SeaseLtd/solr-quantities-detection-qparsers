package com.spaziocodice.labs.solr.qty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.util.NamedList;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
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

    @Before
    public void setUp() throws Exception {
        q =new QuantityDetectionQParserPlugin() {
            @Override
            JsonNode configuration(final ResourceLoader loader) throws IOException {
                return new ObjectMapper().readTree(new File("src/test/resources/assumptions.json"));
            }
        };

        bq = new QuantityDetectionBQParserPlugin() {
            @Override
            JsonNode configuration(final ResourceLoader loader) throws IOException {
                return new ObjectMapper().readTree(new File("src/test/resources/assumptions.json"));
            }
        };

        bf = new QuantityDetectionBFParserPlugin() {
            @Override
            JsonNode configuration(final ResourceLoader loader) throws IOException {
                return new ObjectMapper().readTree(new File("src/test/resources/assumptions.json"));
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
        data.put(" fridge 0.30 ", asList("fridge 0.30","capacity:0.3", "recip(abs(sub(capacity, 0.3)),1,1000,1000)"));
        data.put(" fridge 0.50 ", asList("fridge 0.50","capacity:0.5", "recip(abs(sub(capacity, 0.5)),1,1000,1000)"));
        data.put(" bottle 250 ", asList("bottle 250","capacity:2.5", "recip(abs(sub(capacity, 2.5)),1,1000,1000)"));
        data.put(" fridge 1898 ", asList("fridge 1898","height:1898", "recip(abs(sub(height, 1898)),1,1000,1000)"));

        data.forEach((query, expected) -> {
            final StringBuilder buffer = new StringBuilder(query);
            assertEquals(
                    query,
                    expected.get(0),
                    q.buildQuery(q.queryBuilder(buffer), buffer));

            assertEquals(
                    query,
                    expected.get(1),
                    bq.buildQuery(bq.queryBuilder(buffer), buffer));

            assertEquals(
                    query,
                    expected.get(2),
                    bf.buildQuery(bf.queryBuilder(buffer), buffer));
        });
    }

    @Test
    public void defaultUnit() {
        final Map<String, List<String>> data = new HashMap<>();
        data.put(" fridge 4593", asList("fridge 4593","voltage:4593", "recip(abs(sub(voltage, 4593)),1,1000,1000)"));

        data.forEach((query, expected) -> {
            final StringBuilder buffer = new StringBuilder(query);
            assertEquals(
                    query,
                    expected.get(0),
                    q.buildQuery(q.queryBuilder(buffer), buffer));

            assertEquals(
                    query,
                    expected.get(1),
                    bq.buildQuery(bq.queryBuilder(buffer), buffer));

            assertEquals(
                    query,
                    expected.get(2),
                    bf.buildQuery(bf.queryBuilder(buffer), buffer));
        });
    }

    @Test
    public void noDetectedAmount() {
        final Map<String, List<String>> data = new HashMap<>();
        data.put(" fridge Model no. 1317ABX ", asList("fridge Model no. 1317ABX","", "1"));

        data.forEach((query, expected) -> {
            final StringBuilder buffer = new StringBuilder(query);
            assertEquals(
                    query,
                    expected.get(0),
                    q.buildQuery(q.queryBuilder(buffer), buffer));

            assertEquals(
                    query,
                    expected.get(1),
                    bq.buildQuery(bq.queryBuilder(buffer), buffer));

            assertEquals(
                    query,
                    expected.get(2),
                    bf.buildQuery(bf.queryBuilder(buffer), buffer));
        });
    }
}