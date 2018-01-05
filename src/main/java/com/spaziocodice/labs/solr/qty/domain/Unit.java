package com.spaziocodice.labs.solr.qty.domain;

import java.util.*;

import static java.util.Optional.*;

/**
 * A unit.
 *
 * @author agazzarini
 * @since 1.0
 */
public class Unit {
    /**
     * A unit variant.
     *
     * @author agazzarini
     * @since 1.0
     */
    public class Variant implements Comparable<Variant>{
        final String refName;
        final List<String> syn;

        /**
         * Builds a new variant with the given data.
         *
         * @param refName the variant reference name.
         * @param syn the list of synonyms / forms for this variant.
         */
        Variant(final String refName, final List<String> syn) {
            this.refName = refName;
            this.syn = syn;
        }

        /**
         * Returns the reference name of this variant.
         *
         * @return the reference name of this variant.
         */
        public String refName() {
            return refName;
        }

        /**
         * Returns the alternative forms for this variant.
         *
         * @return the alternative forms for this variant.
         */
        public List<String> forms() {
            return syn;
        }

        @Override
        public int hashCode() {
            return refName.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Variant && ((Variant)obj).refName.equals(refName);
        }

        @Override
        public int compareTo(final Variant v) {
            return refName.compareTo(v.refName);
        }
    }

    /**
     * Gap configuration for range queries associated with this unit.
     *
     * @author agazzarini
     * @since 1.0
     */
    public class Gap {
        final Number value;
        final GapMode mode;

        /**
         * Builds a new gap configuration with the given data.
         *
         * @param value the gap value.
         * @param mode the gap mode.
         * @see GapMode
         */
        Gap(final Number value, final GapMode mode) {
            this.value = value;
            this.mode = mode;
        }

        /**
         * Returns the value of this gap instance.
         *
         * @return the value of this gap instance.
         */
        public Number value() {
            return value;
        }

        /**
         * Returns the mode of this gap instance.
         *
         * @return the mode of this gap instance.
         */
        public GapMode mode() {
            return mode;
        }
    }

    private final String fieldName;
    private final String name;
    private final Optional<Float> boost;

    private Set<Variant> variants = new HashSet<>();
    private Gap gap;
    private final Variant itself;

    /**
     * Builds a new unit with the given data.
     *
     * @param fieldName the (schema) field name associated with this unit.
     * @param name the unit name.
     * @param boost the optional boost associated with this unit.
     */
    public Unit(final String fieldName, final String name, final Float boost) {
        this.fieldName = fieldName;
        this.name = name;
        this.boost = boost == null || boost.equals(1f) ? empty() : of(boost);
        this.itself = new Variant(name, Collections.emptyList());
    }

    /**
     * Associates a new gap with this unit.
     *
     * @param value the gap value.
     * @param mode the gap mode.
     */
    public void setGap(final Number value, final String mode) {
        this.gap = new Gap(value, GapMode.valueOf(mode));
    }

    /**
     * Returns the gap associated with this unit.
     * Note that an Optional is returned, meaning that a gap couldn't have been defined for this unit.
     *
     * @return the gap associated with this unit.
     */
    public Optional<Gap> gap() {
        return ofNullable(gap);
    }

    /**
     * Returns the boost associated with this unit.
     * Note that an Optional is returned, meaning that a boost couldn't have been defined for this unit.
     *
     * @return the boost associated with this unit.
     */
    public Optional<Float> boost() {
        return boost;
    }

    /**
     * Adds a new variant to this unit.
     *
     * @param name the variant reference name.
     * @param syn the list of variant forms.
     */
    public void addVariant(final String name, final List<String> syn) {
        variants.add(new Variant(name, syn));
    }

    /**
     * Returns the unit name.
     *
     * @return the unit name.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the field name associated with this unit.
     *
     * @return the field name associated with this unit.
     */
    public String fieldName() {
        return fieldName;
    }

    /**
     * Returns the variants list asscociated with this unit.
     *
     * @return the variants list asscociated with this unit.
     */
    public Set<Variant> variants() {
        return variants;
    }

    /**
     * Returns the variant associated with the given unitName.
     *
     * @param unitName the unit name.
     * @return the variant associated with the given unitName.
     */
    public Optional<Variant> getVariantByName(final String unitName) {
        final Optional<Variant> result = variants.stream()
                .filter(variant ->  variant.refName.equals(unitName) || variant.syn.contains(unitName))
                .findFirst();
        return result.isPresent()
                ? result
                : unitName.equals(name) ? Optional.of(itself) : Optional.empty();
    }
}