package dev.maltsev.money.transfer.api.service.exception;

/**
 * Thrown when transaction is invalid to be processed.
 */
public class InvalidTransactionException extends Exception {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
