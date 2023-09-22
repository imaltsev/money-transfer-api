package dev.maltsev.money.transfer.api.domain.json;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.object.Money;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.domain.object.TransactionType;

import java.time.LocalDateTime;
import java.util.UUID;

public final class TransferRequest extends AbstractRequest {
    private final String recipientAccountNumber;
    private final String recipient;

    public TransferRequest(
            @JsonProperty("requestId") UUID requestId,
            @JsonProperty("payerAccountNumber") String payerAccountNumber,
            @JsonProperty("recipientAccountNumber") String recipientAccountNumber,
            @JsonProperty("recipient") String recipient,
            @JsonProperty("amount") @JsonDeserialize(using = MoneyDeserializer.class) Money amount
    ) {
        super(requestId, payerAccountNumber, amount);

        if (recipientAccountNumber == null || recipientAccountNumber.isBlank()) {
            throw new IllegalArgumentException("recipientAccountNumber can't be null or blank");
        }

        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("recipient can't be null or blank");
        }

        if (payerAccountNumber.equalsIgnoreCase(recipientAccountNumber)) {
            throw new IllegalArgumentException("payerAccountNumber and recipientAccountNumber can't be the same");
        }

        this.recipientAccountNumber = recipientAccountNumber;
        this.recipient = recipient;
    }

    @Override
    public Transaction toTransaction(String payer) {
        if (payer.isBlank()) {
            throw new IllegalArgumentException("payer can't be null or blank");
        }
        LocalDateTime now = LocalDateTime.now();
        return new Transaction(
                UUID.randomUUID().toString(),
                requestId().toString(),
                payer,
                payerAccountNumber(),
                recipient,
                recipientAccountNumber,
                null,
                amount(),
                TransactionType.TRANSFER,
                TransactionStatus.PROCESSING,
                null,
                now,
                now
        );
    }

    public String recipientAccountNumber() {
        return recipientAccountNumber;
    }

    public String recipient() {
        return recipient;
    }
}