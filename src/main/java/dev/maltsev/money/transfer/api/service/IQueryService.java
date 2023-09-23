package dev.maltsev.money.transfer.api.service;

import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.service.exception.UnknownTransactionException;

import java.util.List;

/**
 * Read-only operations for transactions
 */
public interface IQueryService {
    /**
     * Returns the status of the transaction.
     *
     * @param transactionId the ID of the transaction
     * @param payer         the login of the customer who initiated the transaction
     * @return the status of the transaction
     * @throws UnknownTransactionException if the transaction with the specified ID and payer doesn't exist
     */
    TransactionStatus getTransactionStatus(String transactionId, String payer) throws UnknownTransactionException;

    /**
     * Returns all transactions that are stuck in the PROCESSING state.
     *
     * @return the list of stuck transactions
     */
    List<Transaction> getAllStuckTransactions();
}
