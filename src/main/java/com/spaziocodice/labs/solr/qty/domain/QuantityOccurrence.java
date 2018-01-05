package com.spaziocodice.labs.solr.qty.domain;

/**
 * A simple value object encapsulating a quantity occurrence within a (query) string.
 *
 * @author agazzarini
 * @since 1.0
 * @see <a href="https://martinfowler.com/tags/analysis%20patterns.html">Analysis Patterns</a>
 * @see <a href="http://www.dsc.ufcg.edu.br/~jacques/cursos/map/recursos/fowler-ap/Analysis%20Pattern%20Quantity.htm>Quantity Pattern</a>
 */
public class QuantityOccurrence implements Comparable<QuantityOccurrence> {
    private final Number amount;
    private final String unit;
    private final String fieldName;
    private final int indexOfAmount;
    private final int indexOfUnit;

    /**
     * Builds a new {@link QuantityOccurrence} with the given data.
     *
     * @param amount the amount.
     * @param unit the unit.
     * @param fieldName the field name in the Solr schema.
     * @param indexOfAmount the start index of the amount within the (query) string.
     * @param indexOfUnit the start index of the unit within the (query) string.
     */
    private QuantityOccurrence(final Number amount, final String unit, final String fieldName, final int indexOfAmount, final int indexOfUnit) {
        this.amount = amount;
        this.unit = unit;
        this.fieldName = fieldName;
        this.indexOfAmount = indexOfAmount;
        this.indexOfUnit = indexOfUnit;
    }

    /**
     * Creates a new {@link QuantityOccurrence}.
     *
     * @param amount the occurrence amount.
     * @param unit the associated unit.
     * @param fieldName the field name in the schema.
     * @param indexOfUnit the start offset of the unit (within the input query).
     * @param indexOfAmount the start offset of the amount (within the input query).
     * @return a new {@link QuantityOccurrence} instance.
     */
    public static QuantityOccurrence newQuantityOccurrence(final Number amount, final String unit, final String fieldName, final int indexOfUnit, final int indexOfAmount) {
        return new QuantityOccurrence(amount, unit, fieldName, indexOfAmount, indexOfUnit);
    }

    /**
     * Creates a new {@link QuantityOccurrence}.
     *
     * @param amount the occurrence amount.
     * @param unit the associated unit.
     * @param fieldName the field name in the schema.
     * @return a new {@link QuantityOccurrence} instance.
     */
    public static QuantityOccurrence newQuantityOccurrence(final Number amount, final String unit, final String fieldName) {
        return new QuantityOccurrence(amount, unit, fieldName, -1, -1);
    }

    @Override
    public String toString() {
        return amount + " " + unit + "(" + fieldName + ")" + indexOfAmount + "," + indexOfUnit;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof QuantityOccurrence
                && ((QuantityOccurrence)obj).amount.equals(amount)
                && ((QuantityOccurrence)obj).unit.equals(unit)
                && ((QuantityOccurrence)obj).indexOfAmount == indexOfAmount
                && ((QuantityOccurrence)obj).indexOfUnit == indexOfUnit;
    }

    @Override
    public int hashCode() {
        return amount.hashCode() +
                unit.hashCode() +
                Integer.valueOf(indexOfAmount).hashCode() +
                Integer.valueOf(indexOfUnit).hashCode();
    }

    @Override
    public int compareTo(final QuantityOccurrence occurrence) {
        return occurrence.indexOfAmount - indexOfAmount;
    }

    /**
     * Returns the unit (name) associated with this occurrence.
     *
     * @return the unit (name) associated with this occurrence.
     */
    public String unit() {
        return unit;
    }

    public int indexOfAmount() {
        return indexOfAmount;
    }

    public int indexOfUnit() {
        return indexOfUnit;
    }

    public Number amount() {
        return amount;
    }

    public String fieldName() {
        return fieldName;
    }
}