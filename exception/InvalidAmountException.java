package exception;

/**
 * Thrown when a user enters a monetary amount that can't be acted on:
 * either it isn't a valid number at all, or it's zero/negative.
 *
 * This is a CHECKED exception (extends Exception, not RuntimeException) by
 * design, per the assignment's requirement for a custom checked exception.
 * That means every method that can throw it must either handle it (with a
 * try/catch) or declare it in its own {@code throws} clause — the compiler
 * enforces that callers can't silently ignore it.
 *
 * Deliberately NOT used for "insufficient funds" — that's a normal business
 * outcome (the request was well-formed, there just wasn't enough money),
 * so it's handled as a simple conditional check with a printed message
 * instead of an exception. This exception is reserved for malformed input.
 */
public class InvalidAmountException extends Exception {

    /**
     * @param message a human-readable explanation, printed directly to the
     *                 user when this exception is caught (e.g. "Amount must
     *                 be positive.")
     */
    public InvalidAmountException(String message) {
        super(message);
    }
}