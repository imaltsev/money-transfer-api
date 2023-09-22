package dev.maltsev.money.transfer.api.service.exception;

/**
 * Thrown when the client request is invalid.
 */
public class InvalidClientRequestException extends Exception {
    public InvalidClientRequestException(String message) {
        super(message);
    }
}
