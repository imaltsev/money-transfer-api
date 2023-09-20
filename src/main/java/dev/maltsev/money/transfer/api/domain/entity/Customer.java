package dev.maltsev.money.transfer.api.domain.entity;

import lombok.NonNull;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ToString
@SuppressWarnings("ClassCanBeRecord")
public final class Customer {
    private final String login;
    private final List<Account> accounts;

    public Customer(@NonNull String login) {
        this(login, new ArrayList<>());
    }

    public Customer(@NonNull String login, @NonNull List<Account> accounts) {
        this.login = login;
        this.accounts = new ArrayList<>(accounts);
    }

    public String login() {
        return login;
    }

    public List<Account> accounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Customer addAccount(Account account) {
        accounts.add(account);
        return this;
    }
}

