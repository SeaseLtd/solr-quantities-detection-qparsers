package io.sease.solr.qty.domain;

import java.util.OptionalInt;

/**
 * A simple int tuple.
 *
 * @author agazzarini
 * @since 1.0
 */
public class IntPair {
    private final int x;
    private final OptionalInt y;

    /**
     * Buidls a new {@link IntPair} with the given ints.
     *
     * @param x the first member.
     * @param y the second member.
     */
    public IntPair(final int x, final OptionalInt y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns true if this pair contains valid ints.
     *
     * @return true if this pair contains valid ints.
     */
    public boolean isValid() {
        return y.isPresent();
    }

    /**
     * Returns the first member.
     *
     * @return the first member.
     */
    public int x() {
        return x;
    }

    /**
     * Returns the second member.
     *
     * @return the second member.
     */
    public int y() {
        return y.getAsInt();
    }
}