package io.sease.solr.qty.domain;

import java.util.List;

import static java.lang.Float.parseFloat;

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
    private final List<String> fieldNames;
    private final int indexOfAmount;
    private final int indexOfUnit;
    private final int amountLength;

    /**
     * Builds a new {@link QuantityOccurrence} with the given data.
     *
     * @param amount the amount.
     * @param unit the unit.
     * @param fieldNames the field names in the Solr schema.
     * @param indexOfAmount the start index of the amount within the (query) string.
     * @param indexOfUnit the start index of the unit within the (query) string.
     */
    private QuantityOccurrence(final String amount, final String unit, final List<String> fieldNames, final int indexOfAmount, final int indexOfUnit) {
        this.amount = parseFloat(amount.trim());
        this.unit = unit;
        this.fieldNames = fieldNames;
        this.indexOfAmount = indexOfAmount;
        this.indexOfUnit = indexOfUnit;
        amountLength = this.indexOfUnit == -1 ? amount.length() : -1;
    }

    /**
     * Builds a new {@link QuantityOccurrence} with the given data.
     *
     * @param amount the amount.
     * @param unit the unit.
     * @param fieldNames the field names in the Solr schema.
     * @param indexOfAmount the start index of the amount within the (query) string.
     * @param indexOfUnit the start index of the unit within the (query) string.
     */
    private QuantityOccurrence(final Number amount, final String unit, final List<String> fieldNames, final int indexOfAmount, final int indexOfUnit) {
        this.amount = amount;
        this.unit = unit;
        this.fieldNames = fieldNames;
        this.indexOfAmount = indexOfAmount;
        this.indexOfUnit = indexOfUnit;
        amountLength = -1;
    }

    /**
     * Creates a new {@link QuantityOccurrence}.
     *
     * @param amount the occurrence amount.
     * @param unit the associated unit.
     * @param fieldNames the field names in the schema.
     * @param indexOfUnit the start offset of the unit (within the input query).
     * @param indexOfAmount the start offset of the amount (within the input query).
     * @return a new {@link QuantityOccurrence} instance.
     */
    public static QuantityOccurrence newQuantityOccurrence(
            final String amount,
            final String unit,
            final List<String> fieldNames,
            final int indexOfUnit,
            final int indexOfAmount) {
        return new QuantityOccurrence(amount, unit, fieldNames, indexOfAmount, indexOfUnit);
    }

    /**
     * Creates a new {@link QuantityOccurrence} with no offsets.
     *
     * @param amount the occurrence amount.
     * @param unit the associated unit.
     * @param fieldNames the field names in the schema.
     * @return a new {@link QuantityOccurrence} instance.
     */
    public static QuantityOccurrence newQuantityOccurrence(
            final String amount,
            final String unit,
            final List<String> fieldNames) {
        return newQuantityOccurrence(amount, unit, fieldNames, -1, -1);
    }

    /**
     * Creates a new {@link QuantityOccurrence} with no offsets.
     *
     * @param amount the occurrence amount.
     * @param unit the associated unit.
     * @param fieldNames the field names in the schema.
     * @return a new {@link QuantityOccurrence} instance.
     */
    public static QuantityOccurrence newQuantityOccurrence(
            final Number amount,
            final String unit,
            final List<String> fieldNames) {
        return new QuantityOccurrence(amount, unit, fieldNames, -1, -1);
    }

    @Override
    public String toString() {
        return amount + " " + unit + " maps to " + fieldNames + " [" + indexOfAmount + "," + indexOfUnit + "]";
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
     * Returns the start offset of this occurrence.
     *
     * @return the start offset of this occurrence.
     */
    public int startOffset() {
        return indexOfAmount;
    }

    /**
     * Returns the end offset of this occurrence.
     *
     * @return the end offset of this occurrence.
     */
    public int endOffset() {
        return indexOfUnit != -1 ? indexOfUnit + unit.length() : indexOfAmount + amountLength + 1;
    }

    /**
     * Returns the unit (name) associated with this occurrence.
     *
     * @return the unit (name) associated with this occurrence.
     */
    public String unit() {
        return unit;
    }

    /**
     * Returns the amount associated with this occurrence.
     *
     * @return the amount associated with this occurrence.
     */
    public Number amount() {
        return amount;
    }
}