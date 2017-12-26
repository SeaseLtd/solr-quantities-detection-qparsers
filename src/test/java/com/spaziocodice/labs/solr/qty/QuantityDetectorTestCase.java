package com.spaziocodice.labs.solr.qty;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.assertEquals;

/**
 * {@link QuantityDetector} test case.
 *
 * @author agazzarini
 * @since 1.0
 */
public class QuantityDetectorTestCase {
    private final String unit = "lt";
    private QuantityDetector cut;

    @Before
    public void setUp() {
        cut = new QuantityDetector() {
            @Override
            QueryBuilder queryBuilder(StringBuilder query) {
                return null;
            }
        };
    }

    /**
     * If a unit never occurs, then the result must be an empty list.
     */
    @Test
    public void indexesOfWithoutOccurrence() {
        final String [] queries = {
                "",
                " ",
                "  ",
                "This is a sample query where we don't have the quantity",
                "This is another similar example: there's no alt or lta"
        };

        stream(queries)
                .map(StringBuilder::new)
                .forEach(query -> {
                    final List<Integer> offsets = cut.indexesOf(query, unit);
                    assertEquals(0, offsets.size());
                });
    }

    /**
     * If a unit occurs only once, then the result must be a list with one match.
     */
    @Test
    public void indexesOfWithSingleOccurrence() {
        final String [] queries = {
                "This is a sample query where we have the term lt only once",
                "This is another similar example: only one lt",
                "lt here is still only at the beginning of the query",
                "another lt, so only one match, even if the following words contain it: alternative, telco olt, lta"
        };

        stream(queries)
                .map(StringBuilder::new)
                .forEach(query -> {
            final List<Integer> offsets = cut.indexesOf(query, unit);
            assertEquals(1, offsets.size());
            assertEquals(query.indexOf("lt"), offsets.iterator().next().intValue());
        });
    }

    /**
     * If a unit occurs more than once, then the result must include all matches.
     */
    @Test
    public void indexesOfWithTwoOccurrences() {
        final String [] queries = {
                "This is a sample query where lt is repeated twice. This is the second lt",
                "lt is always lt, nevertheless",
                "lt here is at the beginning and, the lt, in the middle of the query",
                "another lt case, with two matches (this is the second lt)"
        };

        stream(queries)
                .map(StringBuilder::new)
                .forEach(query -> {
                    final List<Integer> offsets = cut.indexesOf(query, unit);
                    assertEquals(2, offsets.size());
                });
    }

    /**
     * If a unit occurs more than once, then the result must include all matches.
     */
    @Test
    public void indexesOfWithThreeOccurrences() {
        final String [] queries = {
                "lt, another lt and finally a last lt",
                "this is the same query (more or less): lt, another lt and finally a last lt",
                "lt, filters, lt and another lt. Instead ltaaa doesn't count, and alta doesn't match as well"
        };

        stream(queries)
                .map(StringBuilder::new)
                .forEach(query -> {
                    final List<Integer> offsets = cut.indexesOf(query, unit);
                    assertEquals(3, offsets.size());
                });
    }

    @Test
    public void unitWithoutQuantity() {
        final String [] queries = {
                "",
                " ",
                "  ",
                "lt",
                "   lt",
                " a  lt",
                "lt",
                "this is not a quantity lt",
                "yet another lt fake quantity",
                "abcdelt or abcde lt",
        };
        stream(queries)
                .map(StringBuilder::new)
                .forEach(query ->
                            assertEquals(
                                query.toString(),
                                -1,
                                cut.startIndexOfAmount(query, query.indexOf(unit))));
    }

    @Test
    public void quantityAtTheVeryBeginning() {
        final String [] queries = {
                "100lt",
                " 100 lt",
                "  100 lt",
                "   100 lt"
        };
        range(0, queries.length)
                .forEach(index -> {
                        final StringBuilder query = new StringBuilder(queries[index]);
                        assertEquals(
                                index + " => " + ">" + query.toString() + "<",
                                index,
                                cut.startIndexOfAmount(query, query.indexOf(unit)));
                });
    }
}