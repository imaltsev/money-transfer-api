package dev.maltsev.money.transfer.api.service.exception;

import dev.maltsev.money.transfer.api.verticle.InvalidClientRequestException;

public class InvalidTransferRequestException extends InvalidClientRequestException {
    public InvalidTransferRequestException(String message) {
        super(message);
    }
}
