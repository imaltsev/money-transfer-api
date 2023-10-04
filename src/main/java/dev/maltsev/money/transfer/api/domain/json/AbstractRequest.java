package dev.maltsev.money.transfer.api.domain.json;


import dev.maltsev.money.transfer.api.domain.object.Money;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode
public abstract class AbstractRequest implements Request {
    private final UUID requestId;
    private final String payerAccountNumber;
    private final Money amount;

    public AbstractRequest(UUID requestId, String payerAccountNumber, Money amount) {
        if (requestId == null) {
            throw new IllegalArgumentException("requestId can't be null");
        }
        if (payerAccountNumber == null || payerAccountNumber.isBlank()) {
            throw new IllegalArgumentException("payerAccountNumber can't be null or blank");
        }

        if (amount == null) {
            throw new IllegalArgumentException("amount can't be null");
        }
        this.requestId = requestId;
        this.payerAccountNumber = payerAccountNumber;
        this.amount = amount;
    }

    public UUID requestId() {
        return requestId;
    }

    public String payerAccountNumber() {
        return payerAccountNumber;
    }

    public Money amount() {
        return amount;
    }
}