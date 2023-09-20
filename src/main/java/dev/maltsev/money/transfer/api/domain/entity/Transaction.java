package dev.maltsev.money.transfer.api.domain.entity;

import dev.maltsev.money.transfer.api.domain.object.Money;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.domain.object.TransactionType;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.LocalDateTime;

@ToString
@SuppressWarnings("ClassCanBeRecord")
public final class Transaction {
    private final String id;
    private final String requestId;
    private final String payer;
    private final String payerAccountNumber;
    private final String recipient;
    private final String recipientAccountNumber;
    private final String withdrawalAddress;
    private final Money amount;
    private final TransactionType type;
    private final LocalDateTime created;
    private TransactionStatus status;
    private String errorMessage;
    private LocalDateTime updated;

    public Transaction(
            @NonNull String id,
            @NonNull String requestId,
            @NonNull String payer,
            @NonNull String payerAccountNumber,
            String recipient,
            String recipientAccountNumber,
            String withdrawalAddress,
            @NonNull Money amount,
            @NonNull TransactionType type,
            @NonNull TransactionStatus status,
            String errorMessage,
            @NonNull LocalDateTime created,
            @NonNull LocalDateTime updated) {
        this.id = id;
        this.requestId = requestId;
        this.payer = payer;
        this.payerAccountNumber = payerAccountNumber;
        this.recipient = recipient;
        this.recipientAccountNumber = recipientAccountNumber;
        this.withdrawalAddress = withdrawalAddress;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.errorMessage = errorMessage;
        this.created = created;
        this.updated = updated;
    }

    public Transaction fail(Exception e) {
        status = TransactionStatus.FAILED;
        errorMessage = ExceptionUtils.getStackTrace(e);
        updated = LocalDateTime.now();
        return this;
    }

    public Transaction fail() {
        status = TransactionStatus.FAILED;
        updated = LocalDateTime.now();
        return this;
    }

    public Transaction complete() {
        status = TransactionStatus.COMPLETED;
        updated = LocalDateTime.now();
        return this;
    }

    public Transaction await() {
        status = TransactionStatus.AWAITING;
        updated = LocalDateTime.now();
        return this;
    }

    public String id() {
        return id;
    }

    public String requestId() {
        return requestId;
    }

    public String payer() {
        return payer;
    }

    public String payerAccountNumber() {
        return payerAccountNumber;
    }

    public String recipient() {
        return recipient;
    }

    public String recipientAccountNumber() {
        return recipientAccountNumber;
    }

    public String withdrawalAddress() {
        return withdrawalAddress;
    }

    public Money amount() {
        return amount;
    }

    public TransactionType type() {
        return type;
    }

    public TransactionStatus status() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public LocalDateTime created() {
        return created;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public LocalDateTime updated() {
        return updated;
    }
}
