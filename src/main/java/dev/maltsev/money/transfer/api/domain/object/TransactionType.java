package dev.maltsev.money.transfer.api.domain.object;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TransactionType {

    WITHDRAWAL("Withdrawal transaction"),
    TRANSFER("Transfer transaction");

    @SuppressWarnings("unused")
    private final String description;
}
