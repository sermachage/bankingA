package model;

import java.math.BigDecimal;

/**
 * Represents one of the bank's customers (Alice, Bob, Charlie, or Diana).
 *
 * A User is really just a bundle of the four "money buckets" described in
 * the spec:
 *   - cash            (starts at $1000, never deposited into the bank yet)
 *   - savingsAccount  (starts empty; grows via interest)
 *   - investmentAccount (starts empty; grows via fund gains)
 *
 * This class holds no business logic of its own (no deposit/withdraw
 * methods) — all operations that move money between these buckets live in
 * BankingService, which orchestrates multiple objects (e.g. moving money
 * from one User's savings to another User's savings). Keeping User itself
 * simple avoids scattering business rules across multiple classes.
 */
public class User {

    // Immutable identity — a user's name never changes after creation.
    private final String name;

    // Cash is mutable because it's the one "bucket" that isn't an Account
    // subclass (it's not stored in a bank account at all, per the spec:
    // "starts with $1000 in cash that is not yet deposited into the bank").
    private BigDecimal cash;

    // Both accounts are created once, here, and never replaced — hence final.
    // Their *contents* (balance) still change over time via their own methods.
    private final SavingsAccount savingsAccount;
    private final InvestmentAccount investmentAccount;

    /**
     * Creates a new user with the starting state defined by the spec:
     * $1000 cash, and two empty accounts.
     *
     * @param name the user's login name (e.g. "Alice")
     */
    public User(String name) {
        this.name = name;
        this.cash = new BigDecimal("1000");
        this.savingsAccount = new SavingsAccount();
        this.investmentAccount = new InvestmentAccount();
    }

    public String getName() {
        return name;
    }

    public BigDecimal getCash() {
        return cash;
    }

    /**
     * Overwrites the user's cash total. Called by BankingService after
     * computing a new value (e.g. {@code cash.subtract(depositAmount)}) —
     * this class doesn't do the arithmetic itself, it just stores the result.
     *
     * @param cash the new cash amount to store
     */
    public void setCash(BigDecimal cash) {
        this.cash = cash;
    }

    public SavingsAccount getSavingsAccount() {
        return savingsAccount;
    }

    public InvestmentAccount getInvestmentAccount() {
        return investmentAccount;
    }
}m