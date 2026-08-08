package model;

import java.math.BigDecimal;

/**
 * The set of investment funds a user can put money into.
 *
 * Each fund is a fixed-risk tier with its own appreciation rate. The rate is
 * stored as a decimal fraction (e.g. 0.02 means 2%), not a percentage, so it
 * can be used directly in a BigDecimal multiplication: balance.multiply(rate).
 *
 * Because this is a Java enum, each constant below (LOW_RISK, MEDIUM_RISK,
 * HIGH_RISK) is a full-fledged object — it runs the constructor with its own
 * argument and can carry its own field, just like any other class instance.
 */
public enum Fund {
    LOW_RISK(new BigDecimal("0.02")),      // 2% appreciation per balance view
    MEDIUM_RISK(new BigDecimal("0.05")),   // 5% appreciation per balance view
    HIGH_RISK(new BigDecimal("0.10"));     // 10% appreciation per balance view

    // Each enum constant gets its own copy of this field, set once via the
    // constructor below and never changed afterward (it's final).
    private final BigDecimal appreciationRate;

    /**
     * Enum constructors are implicitly private — they only ever run once per
     * constant, at class-loading time, and can't be called from outside.
     *
     * @param appreciationRate the fraction (e.g. 0.05 for 5%) this fund's
     *                          balance grows by every time it's applied
     */
    Fund(BigDecimal appreciationRate) {
        this.appreciationRate = appreciationRate;
    }

    /**
     * @return the growth rate for this fund, as a decimal fraction
     */
    public BigDecimal getAppreciationRate() {
        return appreciationRate;
    }
}