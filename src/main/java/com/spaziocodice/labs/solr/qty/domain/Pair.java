package com.spaziocodice.labs.solr.qty.domain;

/**
 * The simplest tuple.
 *
 * @author agazzarini
 * @since 1.0
 */
public class Pair<X,Y> {
    public final X x;
    public final Y y;

    public Pair(final X x, final Y y) {
        this.x = x;
        this.y = y;
    }
}
