package dev.maltsev.money.transfer.api.domain.object;

import lombok.RequiredArgsConstructor;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public enum TransactionStatus {
    PROCESSING("Transaction is currently being processed"),
    AWAITING("Transaction is currently processed on downstream system side"),
    COMPLETED("Transaction has been successfully processed"),
    FAILED("Transaction could not be processed due to an error");

    private final String description;
}