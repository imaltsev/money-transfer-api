package dev.maltsev.money.transfer.api.service;

import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.json.Request;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;

public interface CommandService {

    String createTransaction(Transaction transaction);

    TransactionStatus executeTransaction(String transactionId);
}
