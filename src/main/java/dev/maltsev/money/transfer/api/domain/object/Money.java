package dev.maltsev.money.transfer.api.domain.object;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.math.BigDecimal;

@ToString
@EqualsAndHashCode
@SuppressWarnings("ClassCanBeRecord")
public final class Money implements Comparable<Money> {

    private final BigDecimal value;

    public Money(@NonNull BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount can't be null or negative");
        }
        this.value = value;
    }

    public Money() {
        this(BigDecimal.ZERO);
    }

    public static Money fromInt(int amount) {
        return new Money(new BigDecimal(amount));
    }

    public BigDecimal value() {
        return value;
    }

    @Override
    public int compareTo(Money other) {
        return value.compareTo(other.value);
    }
}
