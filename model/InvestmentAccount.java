package model;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * A user's investment account.
 *
 * This is the most structurally complex model class because it actually
 * tracks TWO layers of money:
 *   1. The account's own balance (inherited from {@link Account}) — money
 *      that has been transferred in from savings but not yet put into a
 *      fund. This is the pool that {@link #investInFund} draws from.
 *   2. Per-fund holdings — money currently invested in LOW_RISK,
 *      MEDIUM_RISK, and HIGH_RISK, tracked separately because each fund
 *      appreciates at its own rate.
 *
 * A user's TOTAL investment wealth at any moment is
 * {@code getBalance() + sum of all fund holdings}, but that total is never
 * stored directly — it's always derived by adding the pieces, which avoids
 * ever having two numbers that could drift out of sync.
 */
public class InvestmentAccount extends Account {

    // Maps each Fund constant to how much money is currently held in it.
    // EnumMap is used (instead of a plain HashMap) because the key set is
    // fixed and known up front (exactly the three Fund constants) — EnumMap
    // is both faster and clearer of intent than a general-purpose map here.
    private final Map<Fund, BigDecimal> fundHoldings;

    /**
     * A new investment account starts with a zero balance (via the
     * Account() superclass constructor) and zero in every fund.
     */
    public InvestmentAccount() {
        super();
        this.fundHoldings = new EnumMap<>(Fund.class);
        for (Fund fund : Fund.values()) {
            fundHoldings.put(fund, BigDecimal.ZERO);
        }
    }

    /**
     * Moves money OUT of the account's own balance and INTO the given fund.
     *
     * The caller (BankingService) is expected to have already validated
     * that the account balance is >= amount before calling this — this
     * method does not re-check, matching how {@link Account#withdraw}
     * behaves (it trusts the caller to have validated sufficiency).
     *
     * @param fund   which fund to invest in
     * @param amount how much to move from the account balance into the fund
     */
    public void investInFund(Fund fund, BigDecimal amount) {
        withdraw(amount); // remove from the investment account's own balance
        fundHoldings.put(fund, fundHoldings.get(fund).add(amount));
    }

    /**
     * Applies one round of appreciation to every fund this account holds
     * money in, each at its own rate (see {@link Fund#getAppreciationRate()}).
     *
     * Like {@link SavingsAccount#applyInterest()}, this is meant to be
     * called only when the user views their balance — repeated views
     * compound the gains, by design.
     */
    public void applyFundGains() {
        for (Fund fund : Fund.values()) {
            BigDecimal current = fundHoldings.get(fund);
            BigDecimal gain = current.multiply(fund.getAppreciationRate());
            fundHoldings.put(fund, current.add(gain));
        }
    }

    /**
     * Withdraws everything from every fund back into the account's own
     * balance, and zeroes out all fund holdings. This matches the spec:
     * "Users can withdraw all their investments at any time, which moves
     * all fund money back to their investment account balance."
     *
     * Note this is an all-or-nothing operation across all three funds —
     * there is no way to withdraw from a single fund individually per the
     * current spec.
     *
     * @return the total amount that was moved back into the account balance
     */
    public BigDecimal withdrawAllInvestments() {
        BigDecimal total = BigDecimal.ZERO;
        for (Fund fund : Fund.values()) {
            total = total.add(fundHoldings.get(fund));
            fundHoldings.put(fund, BigDecimal.ZERO);
        }
        deposit(total);
        return total;
    }

    /**
     * @param fund which fund's holding to look up
     * @return the amount currently held in that fund
     */
    public BigDecimal getFundBalance(Fund fund) {
        return fundHoldings.get(fund);
    }
}