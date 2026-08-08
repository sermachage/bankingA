# Banking App

A command-line banking application built for a Java OOP exercise. Users log in as
one of four predefined people, manage cash / savings / investments, and invest
in three risk-tiered funds that appreciate over time.

## Project Structure

```
bankingApp/
├── BankingApp.java              Entry point. Only static method in the project (main).
├── model/
│   ├── Account.java             Abstract base class: shared balance/deposit/withdraw logic.
│   ├── SavingsAccount.java      extends Account. Adds applyInterest() (1% per balance view).
│   ├── InvestmentAccount.java   extends Account. Tracks per-fund holdings; invest / gain / withdraw-all.
│   ├── Fund.java                enum: LOW_RISK (2%), MEDIUM_RISK (5%), HIGH_RISK (10%).
│   └── User.java                A user's identity: name, cash, savingsAccount, investmentAccount.
├── exception/
│   └── InvalidAmountException.java   Checked exception thrown for non-numeric / non-positive amounts.
├── service/
│   └── BankingService.java      Owns the single Scanner, the user directory, and the whole
│                                 login/menu loop. All CLI I/O and business logic lives here.
└── README.md                    This file.
```

### Why this shape?

- **`model/`** is pure data + behavior — no `System.out`, no `Scanner`, nothing that talks
  to a terminal. You could unit-test every model class without simulating console input.
- **`Account` is abstract** because `SavingsAccount` and `InvestmentAccount` share the same
  core idea (a balance you can deposit into and withdraw from) but diverge in what "grows"
  the balance — interest vs. fund gains — so that growth logic lives in the subclasses.
- **`service/`** is the only place that touches `Scanner` or `System.out`. This keeps I/O
  concerns separate from the money logic, and satisfies the requirement of a single
  `Scanner` instance for the whole program's lifetime (it's a field on `BankingService`,
  created once in the constructor).
- **No static methods except `main`** forces all state (the map of users, the currently
  logged-in user, the scanner, the running flag) to live as instance fields on
  `BankingService`, rather than being smeared across static fields. `BankingApp.main`
  does nothing but construct the service and call `start()`.

## Requirements

- Java 17+ (uses `switch` expressions with arrow syntax; tested on Java 21)
- No external dependencies

## Build & Run

From the `bankingApp/` directory:

```bash
# Compile everything into an out/ directory
javac -d out $(find . -name "*.java")

# Run
cd out
java BankingApp
```

## How It Works

### The four "buckets" of money per user

```
cash (starts at $1000)
  │  deposit (2)          ▲ withdraw (3)
  ▼
savings account (starts $0, +1% every time balance is viewed)
  │  transfer (6)         ▲ transfer (6)
  │  send (4) ──────────► another user's savings
  ▼
investment account (starts $0)
  │  invest in fund (5)   ▲ withdraw all investments (7)
  ▼
fund holdings: LOW_RISK / MEDIUM_RISK / HIGH_RISK
  (each grows by its own rate every time balance is viewed)
```

### Interest & fund gains: applied on *view*, not on a timer

Per the spec, growth is triggered by the act of checking the balance
(menu option 1), not by elapsed time or by other operations. This means:

- Checking the balance 3 times in a row with no other action **compounds** the
  balance 3 times — this is intentional, not a bug.
- Depositing, withdrawing, transferring, sending, or investing do **not** by
  themselves trigger growth; only option 1 does.

This is implemented as `SavingsAccount.applyInterest()` and
`InvestmentAccount.applyFundGains()`, both called at the top of
`BankingService.showBalance()`, before any balance is printed.

### EOF (Ctrl+D) handling

All input goes through a single private method in `BankingService`:

```java
private String readLine() {
    if (!scanner.hasNextLine()) {
        running = false;
        return null;
    }
    return scanner.nextLine();
}
```

Every place that reads input calls `readLine()` and immediately checks for
`null`. If input runs out mid-operation (e.g., the user hits Ctrl+D while
being asked for a deposit amount), the current operation aborts, `running`
becomes `false`, and the main loop exits cleanly. No `System.exit()` is used
anywhere, and no `NoSuchElementException` is ever thrown from `Scanner`.

### Menu-driven flow

`BankingService.start()` is a simple loop:

```java
while (running) {
    if (currentUser == null) {
        login();
    } else {
        showMenuAndHandle();
    }
}
```

Logging out just sets `currentUser = null`, which naturally routes the next
loop iteration back to `login()` — no separate "logged out" state needed.

### Money precision

All balances are `BigDecimal`. Two things worth knowing if you extend this:

- **Never use `BigDecimal.equals()` for value comparison** — it also compares
  scale, so `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` is `false`.
  This codebase uses `compareTo()` everywhere balances are compared
  (e.g., checking sufficient funds).
- Display formatting uses `setScale(2, RoundingMode.HALF_UP)` only when
  printing (see `BankingService.format()`), so internal precision isn't lost
  between operations — only the on-screen representation is rounded.

### Exception handling

`InvalidAmountException` (checked, extends `Exception`) is thrown by
`parseAmount()` for:
- non-numeric input (`NumberFormatException` caught and re-thrown as this)
- zero or negative amounts

**Insufficient funds is deliberately *not* modeled as this exception.** It's
a normal, expected business outcome (not a malformed request), so it's
handled as a plain conditional check with a printed message
(e.g., `"Insufficient cash."`), matching the spec's description of
"displaying appropriate error messages" for that case.

## ⚠️ Known Gap: Exact Output Strings

The assignment states that tests compare output **exactly** — every space,
newline, and character — and that some implementation details are only
revealed in the referenced video, which was not available while building
this. The **behavior and structure** below should be correct:

- Menu header/options text (copied verbatim from the task description,
  including the leading space before `--- Banking App Menu ---`)
- Order of operations, growth-on-view timing, account/fund topology

But these are **best-guess placeholders** you should verify against the
video and adjust if they don't match:

- Login prompt: `"Enter your name to log in:"`
- Login success/failure messages
- Prompt wording for deposit / withdraw / send / invest / transfer
- Fund selection UX (currently expects the fund name typed as text, e.g.
  `HIGH_RISK` — the video may use a numbered menu instead)
- Exact wording of error and confirmation messages
- Whether a message is printed on `Exit` (currently prints `"Goodbye!"`)

See `DESIGN_NOTES.md` for the full list of assumptions and why each one was
made, so you can update them quickly without re-deriving the reasoning.

## Manual Testing

A few scripted runs used during development (piping input via `printf`),
useful as a regression check after you tweak strings:

```bash
# Happy path: login, deposit, transfer to investment, invest, withdraw all, view, logout
printf 'Alice\n1\n2\n500\n1\n6\n1\n200\n1\n5\nHIGH_RISK\n50\n1\n7\n1\n8\n' | java BankingApp

# Error paths: bad username, non-numeric menu choice, negative amount,
# insufficient cash, insufficient savings
printf 'Zed\nAlice\nabc\n2\n-5\n2\n5000\n3\n100\n' | java BankingApp

# Immediate EOF (should exit cleanly, exit code 0, no stack trace)
printf '' | java BankingApp
```

## Possible Bonus Extensions

- Transaction history per user (list of past operations with timestamps)
- Configurable interest/appreciation rates loaded from a properties file
- A `SessionManager` class if session logic grows beyond a single field
- Unit tests for `model/` classes (no I/O needed, so they're easy to test in isolation)