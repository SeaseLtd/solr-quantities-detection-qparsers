package com.spaziocodice.labs.solr.qty.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * A table which encapsulates all (assumption) rules that are used when an orphan amount is detected.
 *
 * @author agazzarini
 * @since 1.0
 */
public class AssumptionTable {
    /**
     * A range associated with a match unit.
     *
     * @author agazzarini
     * @since 1.0
     */
    public static class Range {
        int hashcode;

        final float lowestBound;
        final float highestBound;

        /**
         * Builds a new {@link Range} with the given bounds.
         *
         * @param lowestBound the lowest bound.
         * @param highestBound the highest bound-
         */
        public Range(final float lowestBound, final float highestBound) {
            this.lowestBound = lowestBound;
            this.highestBound = highestBound;
            this.hashcode = Float.valueOf(lowestBound).hashCode() + Float.valueOf(highestBound).hashCode();
        }

        /**
         * Returns true if the given number is falling within the interval represented by this range.
         *
         * @param number the number to test.
         * @return true if the given number is falling within the interval represented by this range.
         */
        boolean includes(final Number number) {
            return number.floatValue() >= lowestBound && number.floatValue() <= highestBound;
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Range
                    && ((Range)obj).lowestBound == lowestBound
                    && ((Range)obj).highestBound == highestBound;
        }

        @Override
        public int hashCode() {
            return hashcode;
        }
    }

    private final Map<Range, String> table = new HashMap<>();
    private final Unit defaultUnit;

    /**
     * Builds a new {@link AssumptionTable} with a default {@link Unit}.
     *
     * @param defaultUnit the default unit that will be used for "orphan" amounts if no other rule is found.
     */
    public AssumptionTable(final Unit defaultUnit) {
        this.defaultUnit = defaultUnit;
    }

    /**
     * Returns true if this table has been enabled on the current instance.
     *
     * @return true if this table has been enabled on the current instance.
     */
    public boolean isEnabled() {
        return !(table.isEmpty() && defaultUnit == Unit.NULL_UNIT);
    }

    /**
     * Adds a new rule to this table.
     * A rule basically associates a unit with a range.
     *
     * @param unit the {@link Unit}.
     * @param range the range.
     */
    public void addRule(final String unit, Range range) {
        table.putIfAbsent(range, unit);
    }

    /**
     * Returns the unit associated with the given amount, according with the rules of this table.
     *
     * @param amount the input amount.
     * @return the unit associated with the given amount, according with the rules in this table.
    */
    public String unitName(final Number amount) {
        return table.keySet()
                .stream()
                .filter(range -> range.includes(amount))
                .map(table::get)
                .findAny()
                .orElse(defaultUnit.name());
    }
}