package io.sease.solr.qty.domain;

/**
 * The simplest tuple.
 *
 * @author agazzarini
 * @since 1.0
 */
public class Pair<X,Y> {
    public final X x;
    public final Y y;

    /**
     * Builds a new Pair with the given data.
     *
     * @param x the first member.
     * @param y the second member.
     */
    Pair(final X x, final Y y) {
        this.x = x;
        this.y = y;
    }
}