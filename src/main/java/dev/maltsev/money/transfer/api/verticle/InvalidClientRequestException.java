package dev.maltsev.money.transfer.api.verticle;

public class InvalidClientRequestException extends Exception {
    public InvalidClientRequestException(String message) {
        super(message);
    }
}
