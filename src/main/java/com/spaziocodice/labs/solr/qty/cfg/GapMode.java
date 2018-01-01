package com.spaziocodice.labs.solr.qty.cfg;

/**
 * Gap mode for range queries.
 * When a gap is defined for a given quantity, range (boost) queries will be enabled for that quantity.
 * The range bounds of the generated query depend on the so called GapMode, specifically:
 *
 * <h3>PIVOT</h3>
 * It means that the detected (quantity amount) will seat in the middle. So if we detect a quantity of 100
 * in the query string (assuming the unit is "height", with a gap mode equal to PIVOT and a gap value of 10,
 * we will have the following range query generated:
 *
 * <p>height:[90 TO 110]</p>
 *
 * <h3>MAX</h3>
 * The detected amount will be used as the higher extreme of the range query. So, assuming the same data of the
 * previous example but a gap mode MAX, the generated range query will be:
 *
 * <p>height:[90 TO 100]</p>
 *
 * @author agazzarini
 * @since 1.0
 */
public enum GapMode {
    PIVOT,MAX
}
