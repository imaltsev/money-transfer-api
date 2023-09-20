package dev.maltsev.money.transfer.api.domain.entity;

import dev.maltsev.money.transfer.api.domain.object.Money;
import lombok.NonNull;
import lombok.ToString;

import java.math.BigDecimal;

@ToString
@SuppressWarnings("ClassCanBeRecord")
public final class Account {
    private final String number;
    private final Money balance;
    private String currency = "USD";

    public Account(@NonNull String number, @NonNull Money balance) {
        this.number = number;
        this.balance = balance;
    }

    public Account(@NonNull String number, @NonNull BigDecimal balance) {
        this.number = number;
        this.balance = new Money(balance);
    }

    public Account(@NonNull String number) {
        this(number, new Money(BigDecimal.ZERO));
    }

    public String number() {
        return number;
    }

    public Money balance() {
        return balance;
    }

    public String currency() {
        return currency;
    }
}