package model;

import java.math.BigDecimal;

/**
 * Abstract base class for any account that holds a monetary balance.
 *
 * Both {@link SavingsAccount} and {@link InvestmentAccount} are, at their
 * core, "a pile of money you can add to or remove from" — that shared
 * behavior lives here so it isn't duplicated in both subclasses. What makes
 * each subclass distinct (interest for savings, fund gains for investments)
 * is added on top of this base.
 *
 * This class deliberately has no knowledge of users, other accounts, or
 * console I/O — it only manages a single BigDecimal balance. Keeping it this
 * "dumb" makes it trivial to unit test in isolation.
 */
public abstract class Account {

    // The current balance. Private so subclasses must go through the
    // protected setter / public deposit-withdraw methods rather than
    // mutating it directly — keeps all balance changes traceable to one path.
    private BigDecimal balance;

    /**
     * Every new account starts empty. (Cash starts at $1000 for a user, but
     * that lives on {@link User}, not here — accounts themselves always
     * start at zero per the spec.)
     */
    public Account() {
        this.balance = BigDecimal.ZERO;
    }

    /**
     * @return the current balance of this account
     */
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Protected (not public) because only this class and its subclasses
     * should ever directly overwrite the balance — external callers should
     * use {@link #deposit(BigDecimal)} / {@link #withdraw(BigDecimal)}
     * instead, which express *why* the balance is changing.
     *
     * @param balance the new balance to store
     */
    protected void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    /**
     * Adds the given amount to the balance. Callers are responsible for
     * validating that the amount is positive before calling this (see
     * {@code BankingService.parseAmount}) — this method just performs the
     * arithmetic.
     *
     * @param amount the amount to add to the balance
     */
    public void deposit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    /**
     * Subtracts the given amount from the balance. Callers are responsible
     * for checking there's enough balance to cover this *before* calling —
     * this method does not itself guard against going negative, since the
     * "insufficient funds" check differs slightly by context (cash vs.
     * savings vs. investment) and is handled at the service layer.
     *
     * @param amount the amount to remove from the balance
     */
    public void withdraw(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }
}