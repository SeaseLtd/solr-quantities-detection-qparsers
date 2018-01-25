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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Equivalences test case.
 *
 * @author agazzarini
 * @since 1.0
 */
public class MultifieldsTestCase {
    private QuantityDetectionBQParserPlugin bq;
    private QuantityDetectionBFParserPlugin bf;
    private final SolrParams params = new ModifiableSolrParams();

    @Before
    public void setUp() throws Exception {
        final JsonNode configuration = new ObjectMapper().readTree(new File("src/test/resources/multifields.json"));

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
    public void oneQuantityWithDifferentBoostsAndGap() {
        asList(
            "There a 100cm quantity here",
            "There a 100 centimeters quantity here",
            "There a 1m quantity here",
            "There a 1 mt quantity here")
            .forEach(query -> {
                    assertEquals(
                            query,
                            "height:100^1.1 width:100^0.2 depth:100^10.0 height:[85 TO 115] width:[100 TO 110] depth:[90 TO 100]",
                            bq.buildQuery(query, params));

                    assertEquals(
                            query,
                            "recip(abs(sub(height, 100)),1,1000,1000) recip(abs(sub(width, 100)),1,1000,1000) recip(abs(sub(depth, 100)),1,1000,1000)",
                            bf.buildQuery(query, params));
                });
    }

    @Test
    public void oneQuantityWithGapAndFloatConversion() {
        asList(
            "There a 100.7cm quantity here",
            "There a 100.7 centimeters quantity here",
            "There a 1.007m quantity here",
            "There a 1.007 mt quantity here")
            .forEach(query -> {
                    assertEquals(
                            query,
                            "height:100.7^1.1 width:100.7^0.2 depth:100.7^10.0 height:[85.7 TO 115.7] width:[100.7 TO 110.7] depth:[90.7 TO 100.7]",
                            bq.buildQuery(query, params));
                    assertEquals(
                        query,
                        "recip(abs(sub(height, 100.7)),1,1000,1000) recip(abs(sub(width, 100.7)),1,1000,1000) recip(abs(sub(depth, 100.7)),1,1000,1000)",
                        bf.buildQuery(query, params));
            });
    }
}