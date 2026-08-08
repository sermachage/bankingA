# Design Notes & Assumptions

This file exists because the task grading is exact-string-match against a
video that wasn't available while writing this code. Rather than guess
silently, every uncertain decision is listed here with *why* it was made,
so you can change just the string/behavior without losing the reasoning —
and so it's obvious which parts are spec-derived (safe) vs. inferred
(verify these).

Legend: 🟢 spec-derived (high confidence) · 🟡 inferred (verify against video)

## Login

- 🟡 Prompt text: `"Enter your name to log in:"`
- 🟡 Success message: `"Welcome, " + name + "!"`
- 🟡 Failure message: `"User not found. Please try again."`
- 🟢 Only Alice, Bob, Charlie, Diana are valid — hardcoded in
  `BankingService`'s constructor as a `LinkedHashMap<String, User>` (insertion
  order preserved, though nothing currently depends on iteration order).

## Menu

- 🟢 Header and option text/order copied verbatim from the task description,
  including the leading space before `--- Banking App Menu ---`.
- 🟡 Invalid-choice message: `"Invalid option. Please try again."`
- 🟡 Whether the menu is reprinted after login vs. only after each completed
  action — currently it's shown once immediately after a successful login
  (via the loop going `login()` → next iteration → `showMenuAndHandle()`),
  then again after every operation.

## Show Balance (option 1)

- 🟢 Growth applied on every view, not on a timer — this is explicit in the
  spec ("Every time users view their account balance...").
- 🟡 Output format/order: currently
  ```
  Cash: $<amount>
  Savings account: $<amount>
  Investment account: $<amount>
  LOW_RISK fund: $<amount>
  MEDIUM_RISK fund: $<amount>
  HIGH_RISK fund: $<amount>
  ```
  The video may show a different line order, different labels, or omit
  zero-balance funds.
- 🟡 Two-decimal formatting (`setScale(2, RoundingMode.HALF_UP)`) — spec
  doesn't state a rounding mode explicitly.

## Deposit (option 2)

- 🟢 Cash → savings only (spec explicitly restricts deposits to savings).
- 🟡 Prompt: `"Enter amount to deposit:"`
- 🟡 Confirmation: `"Deposited $<amount> to savings account."`
- 🟡 Insufficient-cash message: `"Insufficient cash."`

## Withdraw (option 3)

- 🟢 Savings → cash only.
- 🟡 Prompt/confirmation/error text, same pattern as deposit.

## Send Money (option 4)

- 🟢 Moves money from the sender's savings to the recipient's savings.
- 🟡 Whether sending to yourself should be blocked — currently blocked with
  `"You cannot send money to yourself."` The spec doesn't say either way;
  this was a defensive choice, not a spec requirement. Remove the check if
  the video shows self-sends being allowed.
- 🟡 Recipient-not-found message: `"User not found."`

## Invest in Funds (option 5)

- 🟡 **Biggest UX guess in the project.** Currently: type the fund name as
  text (`LOW_RISK`, `MEDIUM_RISK`, or `HIGH_RISK`), case-insensitive
  (`Fund.valueOf(input.trim().toUpperCase())`). The video might instead use
  a numbered sub-menu (1/2/3). If so, swap the parsing in
  `BankingService.investInFund()` — the rest of the logic
  (`InvestmentAccount.investInFund(Fund, BigDecimal)`) doesn't need to change.
- 🟢 Money moves from investment account balance → chosen fund (spec: "using
  money from their investment account").

## Transfer Between Accounts (option 6)

- 🟡 Direction is asked as a sub-choice (`1) Savings to Investment` /
  `2) Investment to Savings`) before the amount. Spec doesn't specify the
  exact interaction; this seemed like the natural minimal UI. Video may
  differ (e.g., ask "from" and "to" as separate account names).

## Withdraw All Investments (option 7)

- 🟢 Moves *all* fund money back to the investment account balance
  (spec is explicit: "moves all fund money back to their investment account
  balance").
- 🟡 Confirmation message text and whether the total withdrawn is shown.

## Logout (option 8) / Exit (option 9)

- 🟡 `"Logged out."` / `"Goodbye!"` — not specified in the task text.
- 🟢 Logout returns to the login prompt without terminating the JVM; Exit
  sets `running = false` and lets `start()`'s loop end naturally
  (no `System.exit()`, as required).

## Cross-Cutting

- 🟢 `BigDecimal` used for every monetary value, everywhere (spec requirement).
- 🟢 Comparisons use `compareTo()`, never `equals()`, to avoid the
  scale-sensitivity trap (`1.0` vs `1.00`).
- 🟢 Single `Scanner` instance, created once in `BankingService`'s
  constructor, passed nowhere else — all reads go through the private
  `readLine()` helper.
- 🟢 `scanner.hasNextLine()` checked before every read; EOF sets a `running`
  flag rather than crashing.
- 🟢 No static methods except `main`.

## Suggested Verification Pass

1. Watch the video once, transcribing every line of console text verbatim
   (prompts, confirmations, errors) into a scratch file.
2. Diff that against the 🟡 items above.
3. Update only the string literals / minor control-flow in
   `BankingService` — the `model/` classes almost certainly don't need to
   change, since they're derived from spec statements marked 🟢 above.
4. Re-run the manual test scripts in `README.md` after each change to catch
   regressions before submitting.