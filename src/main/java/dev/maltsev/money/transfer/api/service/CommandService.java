package dev.maltsev.money.transfer.api.service;

import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.json.Request;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;


/**
 * Operations for handling transactions
 */
public interface CommandService {


    /**
     * Creates a new transaction.
     *
     * @param transaction the transaction to be created
     * @return the ID of the created transaction
     */
    String createTransaction(Transaction transaction);

    /**
     * Executes a transaction.
     *
     * @param transactionId the ID of the transaction to be executed
     * @return the status of the executed transaction
     */
    TransactionStatus executeTransaction(String transactionId);
}
