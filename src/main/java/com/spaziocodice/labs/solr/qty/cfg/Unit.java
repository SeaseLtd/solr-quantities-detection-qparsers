package com.spaziocodice.labs.solr.qty.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class Unit {
    public class Variant {
        final String refName;
        final List<String> syn;

        Variant(final String refName, final List<String> syn) {
            this.refName = refName;
            this.syn = syn;
        }

        public String refName() {
            return refName;
        }

        public List<String> forms() {
            return syn;
        }
    }

    public class Gap {
        final Number value;
        final GapMode mode;

        Gap(final Number value, final GapMode mode) {
            this.value = value;
            this.mode = mode;
        }

        public Number value() {
            return value;
        }

        public GapMode mode() {
            return mode;
        }
    }

    final String fieldName;
    final String name;
    final Float boost;

    List<Variant> variants = new ArrayList<>();
    Gap gap;

    public Unit(final String fieldName, final String name, final Float boost) {
        this.fieldName = fieldName;
        this.name = name;
        this.boost = boost;
    }

    public void setGap(final Number value, final String mode) {
        this.gap = new Gap(value, GapMode.valueOf(mode));
    }

    public Optional<Gap> gap() {
        return ofNullable(gap);
    }

    public void addVariant(final String name, final List<String> syn) {
        variants.add(new Variant(name, syn));
    }

    public String name() {
        return name;
    }

    public String fieldName() {
        return fieldName;
    }

    public List<Variant> variants() {
        return variants;
    }
}