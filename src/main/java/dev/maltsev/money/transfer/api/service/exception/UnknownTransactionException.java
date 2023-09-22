package dev.maltsev.money.transfer.api.service.exception;

/**
* Thrown when transaction with the given ID is not found.
 */
public class UnknownTransactionException extends InvalidClientRequestException {
    public UnknownTransactionException(String message) {
        super(message);
    }
}
