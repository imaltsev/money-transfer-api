package dev.maltsev.money.transfer.api.domain.entity;

import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import lombok.NonNull;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@ToString
@SuppressWarnings("ClassCanBeRecord")
public final class TransactionHistory {
    private final String id;
    private final TransactionStatus status;
    private final LocalDateTime timestamp;

    public TransactionHistory(@NonNull String id, @NonNull TransactionStatus status, @NonNull LocalDateTime timestamp) {
        this.id = id;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String id() {
        return id;
    }

    public TransactionStatus status() {
        return status;
    }

    public LocalDateTime timestamp() {
        return timestamp;
    }
}
