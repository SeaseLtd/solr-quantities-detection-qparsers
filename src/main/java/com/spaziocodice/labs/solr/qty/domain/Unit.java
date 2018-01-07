package com.spaziocodice.labs.solr.qty.domain;

import java.util.*;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

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
         * Builds a new defaultGap configuration with the given data.
         *
         * @param value the defaultGap value.
         * @param mode the defaultGap mode.
         * @see GapMode
         */
        Gap(final Number value, final GapMode mode) {
            this.value = value;
            this.mode = mode;
        }

        /**
         * Returns the value of this defaultGap instance.
         *
         * @return the value of this defaultGap instance.
         */
        public Number value() {
            return value;
        }

        /**
         * Returns the mode of this defaultGap instance.
         *
         * @return the mode of this defaultGap instance.
         */
        public GapMode mode() {
            return mode;
        }
    }

    private final List<String> fieldNames;
    private final String name;
    private Float defaultBoost;
    private final Map<String, Float> boostOverrideMap = new HashMap<>();


    private Set<Variant> variants = new HashSet<>();
    private Gap defaultGap;
    private final Map<String, Gap> gapOverrideMap = new HashMap<>();
    private final Variant itself;

    /**
     * Builds a new unit with the given data.
     *
     * @param fieldNameDef the (schema) field name (s) associated with this unit.
     * @param name the unit name.
     */
    public Unit(final String fieldNameDef, final String name) {
        this.fieldNames =
                stream(fieldNameDef.split(","))
                    .filter(fname -> fname != null && fname.trim().length() > 0)
                    .map(String::trim)
                    .collect(toList());
        this.name = name;
        this.itself = new Variant(name, Collections.emptyList());
    }

    public void setDefaultBoost(final float value) {
        this.defaultBoost = value == 1f  || value == 0f ? null : value;
    }

    public void addBoost(final String fieldName, final float value) {
        if (value > 0f && value != 1f) {
            boostOverrideMap.put(fieldName, value);
        }
    }

    /**
     * Associates a new default defaultGap with this unit.
     *
     * @param value the defaultGap value.
     * @param mode the defaultGap mode.
     */
    public void setGap(final Number value, final String mode) {
        this.defaultGap = new Gap(value, GapMode.valueOf(mode));
    }

    /**
     * Associates a new defaultGap with this a specific fieldname belonging to this unit.
     *
     * @param fieldName the field name.
     * @param value the defaultGap value.
     * @param mode the defaultGap mode.
     */
    public void addGap(final String fieldName, final Number value, final String mode) {
        gapOverrideMap.put(fieldName, new Gap(value, GapMode.valueOf(mode)));
    }

    /**
     * Returns the defaultGap associated with this unit.
     * Note that an Optional is returned, meaning that a defaultGap couldn't have been defined for this unit.
     *
     * @param fieldName the field name, owner of the defaultGap we are looking for
     * @return the defaultGap associated with this unit.
     */
    public Pair<String, Optional<Gap>> gap(final String fieldName) {
        return new Pair(fieldName, ofNullable(gapOverrideMap.getOrDefault(fieldName, defaultGap)));
    }

    /**
     * Returns the defaultBoost associated with this unit.
     * Note that an Optional is returned, meaning that a defaultBoost couldn't have been defined for this unit.
     *
     * @return the defaultBoost associated with this unit.
     */
    public Optional<Float> boost(final String fieldName) {
        return ofNullable(boostOverrideMap.getOrDefault(fieldName, defaultBoost));
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
     * Returns the field names associated with this unit.
     *
     * @return the field names associated with this unit.
     */
    public List<String> fieldNames() {
        return fieldNames;
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