package com.spaziocodice.labs.solr.qty;

/**
 * Utilities collection.
 *
 * @author agazzarini
 * @since 1.0
 */
public abstract class F {
    /**
     * Check the input value and if it doesn't have any decimals, returns it as a plain int.
     *
     * @param value the input value.
     * @return (possibily) the input value as a plain int or float (in case it has decimals).
     */
    public static Number narrow(final Number value) {
        return narrow(value.floatValue());
    }

    /**
     * Check the input value and if it doesn't have any decimals, returns it as a plain int.
     *
     * @param value the input value.
     * @return (possibily) the input value as a plain int or float (in case it has decimals).
     */
    public static Number narrow(final float value) {
        if (value % 1 == 0) return (int)value; else return value;
    }

    /**
     * Returns (possibily) the input value as a plain int.
     *
     * @param value the input value.
     * @return (possibily) the input value as a plain int.
     */
    public static Comparable<? extends Number> narrowAsComparable(final float value) {
        return Float.valueOf(value);
    }

}
