package dev.maltsev.money.transfer.api.domain.json;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.object.Money;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.domain.object.TransactionType;
import lombok.ToString;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@ToString
public final class WithdrawRequest extends AbstractRequest {

    private final String address;

    public WithdrawRequest(
            @JsonProperty("requestId") UUID requestId,
            @JsonProperty("payerAccountNumber") String payerAccountNumber,
            @JsonProperty("address") String address,
            @JsonProperty("amount") @JsonDeserialize(using = MoneyDeserializer.class) Money amount
    ) {
        super(requestId, payerAccountNumber, amount);

        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address can't be null or blank");
        }

        //noinspection ResultOfMethodCallIgnored
        URI.create(address);
        this.address = address;
    }

    @Override
    public Transaction toTransaction(String payer) {
        if (payer.isBlank()) {
            throw new IllegalArgumentException("Payer can't be null or blank");
        }
        LocalDateTime now = LocalDateTime.now();
        return new Transaction(
                UUID.randomUUID().toString(),
                requestId().toString(),
                payer,
                payerAccountNumber(),
                null,
                null,
                address,
                amount(),
                TransactionType.WITHDRAWAL,
                TransactionStatus.PROCESSING,
                null,
                now,
                now
        );
    }

    public String address() {
        return address;
    }
}