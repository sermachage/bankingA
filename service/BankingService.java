package service;

import exception.InvalidAmountException;
import model.Account;
import model.Fund;
import model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * The entire CLI application lives here: it owns the single {@link Scanner}
 * instance for the program's whole lifetime, holds the directory of the
 * four predefined users, tracks who's currently logged in, and implements
 * every menu operation.
 *
 * This is intentionally the ONLY class that touches {@code System.out} or
 * {@code Scanner} — {@code model} classes are pure data/logic with no I/O,
 * which keeps the "what changes the money" logic separable from "how the
 * user is asked for input."
 *
 * Design note on EOF handling: every single read from the scanner goes
 * through {@link #readLine()}. That method checks {@code hasNextLine()}
 * before reading, and if input has run out (e.g. the user pressed Ctrl+D),
 * it flips the {@code running} flag to false and returns null instead of
 * letting {@code Scanner.nextLine()} throw. Every caller of
 * {@code readLine()} checks for null immediately afterward and bails out of
 * the current operation, letting control unwind back to the main loop in
 * {@link #start()}, which then exits naturally — no {@code System.exit()}
 * anywhere.
 */
public class BankingService {

    // The one and only Scanner for the program's entire lifetime, per the
    // spec's requirement. Created once here, in the constructor, and never
    // recreated — every read anywhere in this class goes through this field.
    private final Scanner scanner;

    // The four predefined users, keyed by login name. LinkedHashMap is used
    // (rather than plain HashMap) purely so iteration order matches
    // insertion order (Alice, Bob, Charlie, Diana) if that's ever needed —
    // lookups by name work the same either way.
    private final Map<String, User> users;

    // Null when nobody is logged in. When null, the main loop shows the
    // login prompt; when non-null, it shows the menu for this user.
    private User currentUser;

    // Controls the main loop in start(). Set to false either by choosing
    // "Exit" from the menu, or by hitting EOF on any input read.
    private boolean running;

    /**
     * Sets up the scanner and seeds the four predefined users, each with
     * their starting $1000 cash and two empty accounts (see {@link User}'s
     * constructor for those starting values).
     */
    public BankingService() {
        this.scanner = new Scanner(System.in);
        this.users = new LinkedHashMap<>();
        users.put("Alice", new User("Alice"));
        users.put("Bob", new User("Bob"));
        users.put("Charlie", new User("Charlie"));
        users.put("Diana", new User("Diana"));
        this.currentUser = null;
        this.running = true;
    }

    /**
     * The application's main loop. Alternates between showing the login
     * prompt (when nobody's logged in) and the menu (when someone is),
     * until {@code running} becomes false — either because the user chose
     * "Exit" or because input ran out (EOF).
     *
     * Closes the scanner once the loop ends, since nothing else will read
     * from it after this point.
     */
    public void start() {
        while (running) {
            if (currentUser == null) {
                login();
            } else {
                showMenuAndHandle();
            }
        }
        scanner.close();
    }

    /**
     * The single choke point for all console input in this class.
     *
     * Checks {@code hasNextLine()} before reading so that running out of
     * input (Ctrl+D) is treated as a graceful "stop the program" signal
     * rather than an uncaught {@code NoSuchElementException}.
     *
     * @return the next line of input, or {@code null} if input has run out
     *         (in which case {@code running} has also been set to false as
     *         a side effect, so the main loop will exit on its next check)
     */
    private String readLine() {
        if (!scanner.hasNextLine()) {
            running = false;
            return null;
        }
        return scanner.nextLine();
    }

    /**
     * Prompts for a username and, if it matches one of the four predefined
     * users, logs them in by setting {@code currentUser}. On an unknown
     * name, prints an error and simply returns — the main loop will call
     * this method again on its next iteration, effectively re-prompting.
     */
    private void login() {
        System.out.println("Enter your name to log in:");
        String name = readLine();
        if (name == null) return; // EOF while waiting for a username

        name = name.trim();
        User user = users.get(name);
        if (user == null) {
            System.out.println("User not found. Please try again.");
        } else {
            currentUser = user;
            System.out.println("Welcome, " + name + "!");
        }
    }

    /**
     * Prints the menu, reads the user's chosen option, and dispatches to
     * the matching handler method. Invalid input (non-numeric, or a number
     * outside 1-9) just prints an error and returns, so the main loop shows
     * the menu again on the next iteration.
     */
    private void showMenuAndHandle() {
        printMenu();
        String input = readLine();
        if (input == null) return; // EOF while waiting for a menu choice

        int choice;
        try {
            choice = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid option. Please try again.");
            return;
        }

        // Modern switch-expression style (arrow form) — each case calls
        // exactly one handler method and falls through to nothing else,
        // so there's no risk of accidental fall-through between cases.
        switch (choice) {
            case 1 -> showBalance();
            case 2 -> deposit();
            case 3 -> withdraw();
            case 4 -> sendMoney();
            case 5 -> investInFund();
            case 6 -> transferBetweenAccounts();
            case 7 -> withdrawAllInvestments();
            case 8 -> logout();
            case 9 -> exit();
            default -> System.out.println("Invalid option. Please try again.");
        }
    }

    /**
     * Prints the menu header and all nine options, in the exact order and
     * wording given in the assignment (including the leading space before
     * the header's dashes).
     */
    private void printMenu() {
        System.out.println(" --- Banking App Menu ---");
        System.out.println("1. Show balance");
        System.out.println("2. Deposit money");
        System.out.println("3. Withdraw money");
        System.out.println("4. Send money to a person");
        System.out.println("5. Invest in funds");
        System.out.println("6. Transfer between accounts");
        System.out.println("7. Withdraw all investments");
        System.out.println("8. Logout");
        System.out.println("9. Exit");
    }

    /**
     * Option 1: Show balance.
     *
     * This is the ONLY place where interest and fund gains are triggered —
     * per the spec, growth happens "every time users view their account
     * balance," not on a timer and not as a side effect of other
     * operations. Calling this twice in a row with nothing else happening
     * in between will compound both the interest and the fund gains twice,
     * which is intentional behavior, not a bug.
     */
    private void showBalance() {
        currentUser.getSavingsAccount().applyInterest();
        currentUser.getInvestmentAccount().applyFundGains();

        System.out.println("Cash: $" + format(currentUser.getCash()));
        System.out.println("Savings account: $" + format(currentUser.getSavingsAccount().getBalance()));
        System.out.println("Investment account: $" + format(currentUser.getInvestmentAccount().getBalance()));
        for (Fund fund : Fund.values()) {
            System.out.println(fund + " fund: $" + format(currentUser.getInvestmentAccount().getFundBalance(fund)));
        }
    }

    /**
     * Option 2: Deposit money — moves money from cash into the savings
     * account. Per the spec, cash can ONLY be deposited into savings (not
     * directly into investments), which is reflected in this method only
     * ever touching {@code getSavingsAccount()}.
     */
    private void deposit() {
        System.out.println("Enter amount to deposit:");
        String input = readLine();
        if (input == null) return;

        try {
            BigDecimal amount = parseAmount(input);
            if (currentUser.getCash().compareTo(amount) < 0) {
                System.out.println("Insufficient cash.");
                return;
            }
            currentUser.setCash(currentUser.getCash().subtract(amount));
            currentUser.getSavingsAccount().deposit(amount);
            System.out.println("Deposited $" + format(amount) + " to savings account.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Option 3: Withdraw money — the reverse of deposit: moves money from
     * the savings account back into cash.
     */
    private void withdraw() {
        System.out.println("Enter amount to withdraw:");
        String input = readLine();
        if (input == null) return;

        try {
            BigDecimal amount = parseAmount(input);
            if (currentUser.getSavingsAccount().getBalance().compareTo(amount) < 0) {
                System.out.println("Insufficient funds in savings account.");
                return;
            }
            currentUser.getSavingsAccount().withdraw(amount);
            currentUser.setCash(currentUser.getCash().add(amount));
            System.out.println("Withdrew $" + format(amount) + " to cash.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Option 4: Send money to another person — moves money directly from
     * the current user's savings account to another user's savings account
     * (money never passes through cash for this operation).
     *
     * Sending to yourself is blocked as a defensive choice (not explicitly
     * required or forbidden by the spec) since it would be a no-op that
     * could mask bugs elsewhere; remove the check if the reference
     * implementation allows it.
     */
    private void sendMoney() {
        System.out.println("Enter recipient's name:");
        String name = readLine();
        if (name == null) return;

        name = name.trim();
        User recipient = users.get(name);
        if (recipient == null) {
            System.out.println("User not found.");
            return;
        }
        if (recipient == currentUser) {
            System.out.println("You cannot send money to yourself.");
            return;
        }

        System.out.println("Enter amount to send:");
        String input = readLine();
        if (input == null) return;

        try {
            BigDecimal amount = parseAmount(input);
            if (currentUser.getSavingsAccount().getBalance().compareTo(amount) < 0) {
                System.out.println("Insufficient funds in savings account.");
                return;
            }
            currentUser.getSavingsAccount().withdraw(amount);
            recipient.getSavingsAccount().deposit(amount);
            System.out.println("Sent $" + format(amount) + " to " + recipient.getName() + ".");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Option 5: Invest in funds — moves money from the investment account's
     * own balance into one of the three funds. Two things can go wrong here
     * that DON'T involve {@link InvalidAmountException}: an unrecognized
     * fund name, and an unparseable/non-positive amount (which does use
     * that exception, same as every other money-amount input in this class).
     */
    private void investInFund() {
        System.out.println("Choose a fund: LOW_RISK, MEDIUM_RISK, HIGH_RISK");
        String fundInput = readLine();
        if (fundInput == null) return;

        Fund fund;
        try {
            // Fund.valueOf() requires an exact match to a constant's name;
            // uppercasing the trimmed input makes the prompt case-insensitive
            // from the user's perspective (e.g. "high_risk" still works).
            fund = Fund.valueOf(fundInput.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid fund selected.");
            return;
        }

        System.out.println("Enter amount to invest:");
        String input = readLine();
        if (input == null) return;

        try {
            BigDecimal amount = parseAmount(input);
            if (currentUser.getInvestmentAccount().getBalance().compareTo(amount) < 0) {
                System.out.println("Insufficient funds in investment account.");
                return;
            }
            currentUser.getInvestmentAccount().investInFund(fund, amount);
            System.out.println("Invested $" + format(amount) + " in " + fund + ".");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Option 6: Transfer between accounts — moves money either
     * savings-to-investment or investment-to-savings, depending on the
     * direction the user chooses first.
     *
     * The {@code Account} local variables use the abstract base type
     * deliberately: this method doesn't care whether it's manipulating a
     * SavingsAccount or an InvestmentAccount specifically, only that both
     * support getBalance()/withdraw()/deposit() — a direct illustration of
     * why Account being abstract (rather than the two subclasses being
     * unrelated) is useful.
     */
    private void transferBetweenAccounts() {
        System.out.println("Transfer direction: 1) Savings to Investment  2) Investment to Savings");
        String dirInput = readLine();
        if (dirInput == null) return;

        int direction;
        try {
            direction = Integer.parseInt(dirInput.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid option.");
            return;
        }
        if (direction != 1 && direction != 2) {
            System.out.println("Invalid option.");
            return;
        }

        System.out.println("Enter amount to transfer:");
        String input = readLine();
        if (input == null) return;

        try {
            BigDecimal amount = parseAmount(input);
            Account from = direction == 1 ? currentUser.getSavingsAccount() : currentUser.getInvestmentAccount();
            Account to = direction == 1 ? currentUser.getInvestmentAccount() : currentUser.getSavingsAccount();
            if (from.getBalance().compareTo(amount) < 0) {
                System.out.println("Insufficient funds.");
                return;
            }
            from.withdraw(amount);
            to.deposit(amount);
            System.out.println("Transferred $" + format(amount) + ".");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Option 7: Withdraw all investments — no amount to enter here, since
     * per the spec this always moves EVERYTHING from every fund back into
     * the investment account balance in one action. All the actual work is
     * delegated to {@link model.InvestmentAccount#withdrawAllInvestments()};
     * this method just reports the result.
     */
    private void withdrawAllInvestments() {
        BigDecimal total = currentUser.getInvestmentAccount().withdrawAllInvestments();
        System.out.println("Withdrew $" + format(total) + " from investments to investment account.");
    }

    /**
     * Option 8: Logout — simply clears {@code currentUser}. The main loop
     * in {@link #start()} will naturally route back to {@link #login()} on
     * its next iteration since it checks {@code currentUser == null}; no
     * separate "logged out" state or flag is needed.
     */
    private void logout() {
        System.out.println("Logged out.");
        currentUser = null;
    }

    /**
     * Option 9: Exit — stops the main loop by flipping {@code running} to
     * false. Deliberately does NOT call {@code System.exit()} (the
     * assignment explicitly forbids it, since it would prevent the test
     * harness from completing); instead the loop in {@link #start()} simply
     * ends on its own after this method returns.
     */
    private void exit() {
        running = false;
        System.out.println("Goodbye!");
    }

    /**
     * Parses and validates a raw string as a monetary amount.
     *
     * This is the single validation point used by every operation that asks
     * the user for an amount (deposit, withdraw, send, invest, transfer),
     * so the "must be a real positive number" rule only needs to be written
     * once.
     *
     * @param input the raw text the user typed
     * @return a positive BigDecimal parsed from the input
     * @throws InvalidAmountException if the input isn't a valid number, or
     *                                 is zero/negative
     */
    private BigDecimal parseAmount(String input) throws InvalidAmountException {
        BigDecimal amount;
        try {
            amount = new BigDecimal(input.trim());
        } catch (NumberFormatException e) {
            // BigDecimal's constructor throws NumberFormatException for
            // unparseable text (e.g. "abc") — translated here into our own
            // checked exception so callers only need to catch one type.
            throw new InvalidAmountException("Invalid amount entered.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            // compareTo (not equals!) is used here and everywhere else in
            // this class that compares BigDecimal values, since equals()
            // also compares scale (e.g. 1.0 vs 1.00 would be "not equal"),
            // which is never what we actually want when comparing amounts.
            throw new InvalidAmountException("Amount must be positive.");
        }
        return amount;
    }

    /**
     * Formats a BigDecimal for display: always exactly two decimal places,
     * rounding half-up if there are more.
     *
     * Note this rounding only affects what's PRINTED — the underlying
     * balances stored on accounts keep their full unrounded precision, so
     * repeated operations don't lose precision due to display formatting.
     *
     * @param amount the value to format
     * @return the amount as a string with exactly two decimal places
     */
    private String format(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
}