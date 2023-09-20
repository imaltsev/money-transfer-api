package dev.maltsev.money.transfer.api.service.exception;

import dev.maltsev.money.transfer.api.verticle.InvalidClientRequestException;

public class UnknownTransactionException extends InvalidClientRequestException {
    public UnknownTransactionException(String message) {
        super(message);
    }
}
