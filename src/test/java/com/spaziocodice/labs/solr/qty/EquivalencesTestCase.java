package com.spaziocodice.labs.solr.qty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Equivalences test case.
 *
 * @author agazzarini
 * @since 1.0
 */
public class EquivalencesTestCase {
    private QuantityDetectionBQParserPlugin bq;
    private QuantityDetectionBFParserPlugin bf;
    private final SolrParams params = new ModifiableSolrParams();

    @Before
    public void setUp() throws Exception {
        final JsonNode configuration = new ObjectMapper().readTree(new File("src/test/resources/equivalences.json"));

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

        bq.init(mock(NamedList.class));
        bq.inform(mock(ResourceLoader.class));

        bf.init(mock(NamedList.class));
        bf.inform(mock(ResourceLoader.class));
    }

    @Test
    public void oneQuantityWithIntegerConversion() {
        asList(
            "There's a 1lt quantity here",
            "There's a 1 l quantity here",
            "There's a 100cl quantity here",
            "There's a 1000 ml quantity here")
            .forEach(q ->
                        assertEquals(
                                q,
                                "capacity:1",
                                bq.buildQuery(q, params)));
    }

    @Test
    public void oneQuantityWithFloatConversion() {
        asList(
            "There's a 1.2lt quantity here",
            "There's a 1.2 l quantity here",
            "There's a 120 cl quantity here",
            "There's a 1200 ml quantity here")
            .forEach(query ->
                    assertEquals(
                            query,
                            "capacity:1.2",
                            bq.buildQuery(query, params)));
    }

    @Test
    public void oneQuantityWithGapAndIntegerConversion() {
        asList(
            "There's a 100cm quantity here",
            "There's a 100 centimeters quantity here",
            "There's a 1m quantity here",
            "There's a 1 mt quantity here")
            .forEach(query -> {
                    assertEquals(
                            query,
                            "height:100 height:[90 TO 110]",
                            bq.buildQuery(query, params));

                    assertEquals(
                            query,
                            "recip(abs(sub(height, 100)),1,1000,1000)",
                            bf.buildQuery(query, params));
                });
    }

    @Test
    public void oneQuantityWithGapAndFloatConversion() {
        asList(
            "There's a 100.7cm quantity here",
            "There's a 100.7 centimeters quantity here",
            "There's a 1.007m quantity here",
            "There's a 1.007 mt quantity here")
                .forEach(query -> {
                        assertEquals(
                                query,
                                "height:100.7 height:[90.7 TO 110.7]",
                                bq.buildQuery(query, params));
                        assertEquals(
                            query,
                            "recip(abs(sub(height, 100.7)),1,1000,1000)",
                            bf.buildQuery(query, params));
                });
    }
}