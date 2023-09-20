package dev.maltsev.money.transfer.api.domain.json;

import dev.maltsev.money.transfer.api.domain.entity.Transaction;

public interface Request {
    Transaction toTransaction(String login);
}
