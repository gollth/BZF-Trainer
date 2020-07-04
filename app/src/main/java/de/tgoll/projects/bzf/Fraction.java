package de.tgoll.projects.bzf;

import androidx.annotation.Nullable;

/**
 * A rational number represented by a denominator and a nominator
 */
class Fraction {

    /** The denominotor, i.e. the number above the fraction */
    public int denominator;

    /** The nominator, i.e. the number below the fraction */
    public int nominator;

    /** Deep clone a fraction object */
    public Fraction(@Nullable Fraction other) {
        this.denominator = other == null ? 0 : other.denominator;
        this.nominator = other == null ? 0 : other.nominator;
    }

    /**
     * Create a new fraction from two number
     * @param denominator the number above the fraction
     * @param nominator the number below the fraction
     */
    public Fraction(int denominator, int nominator) {
        this.denominator = denominator;
        this.nominator = nominator;
    }

    /**
     * Calculate the floating point rational number by dividing {@link #denominator} by {@link #nominator}
     * @return the fraction as float
     */
    public float toRational() {
        return (float)this.denominator / this.nominator;
    }

}
