package com.spaziocodice.labs.solr.qty.domain;

import java.util.Collections;
import java.util.Map;

import static com.spaziocodice.labs.solr.qty.F.narrow;

/**
 * A collection of equivalence rules for converting amounts of different units.
 *
 * @author agazzarini
 * @since 1.0
 */
public class EquivalenceTable {
    private final Map<String, Number> table;

    /**
     * Builds a new equivalence table with the given rules data.
     * The input data is a simple {@link Map} pairing a unit (name) with a conversion factor.
     *
     * @param data the unit-factor pairs.
     */
    public EquivalenceTable(final Map<String, Number> data) {
        this.table = Collections.unmodifiableMap(data);
    }

    /**
     * Performs the equivalence according with the given data and this equivalence table.
     *
     * @param unitName the unit name
     * @param amount the amount that will be converted.
     * @return the converted amount, according with the rules configured in this equivalence table.
     */
    public Number equivalent(final String unitName, final Number amount ) {
        final float factor = table.getOrDefault(unitName, 1).floatValue();
        return narrow(factor > 0 ? amount.floatValue() / factor : amount.floatValue() * factor);
    }
}
