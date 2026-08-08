package model;

import java.math.BigDecimal;

/**
 * A user's savings account. This is the "hub" account: cash is deposited
 * into it, money can be withdrawn back to cash, sent to other users, or
 * transferred into the user's investment account.
 *
 * Its one piece of special behavior beyond {@link Account} is interest,
 * which compounds every time the balance is *viewed* (not on a timer, and
 * not automatically after every operation — see {@link #applyInterest()}).
 */
public class SavingsAccount extends Account {

    // 1% interest rate, expressed as a decimal fraction so it can be used
    // directly in a BigDecimal multiplication (balance * 0.01).
    // static + final: this rate is a property of "savings accounts" in
    // general, not of any individual account instance, and never changes.
    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.01");

    /**
     * Applies one round of interest to the current balance.
     *
     * Per the spec, this must be called every time the user views their
     * account balance — nowhere else. That means:
     *   - Viewing the balance 3 times in a row (with no other action
     *     between views) compounds interest 3 times. This is intentional.
     *   - Depositing, withdrawing, sending, or transferring money does NOT
     *     itself trigger interest — only a balance view does.
     *
     * The caller (BankingService.showBalance) is responsible for invoking
     * this at the right time; this method just does the math.
     */
    public void applyInterest() {
        BigDecimal interest = getBalance().multiply(INTEREST_RATE);
        deposit(interest);
    }
}