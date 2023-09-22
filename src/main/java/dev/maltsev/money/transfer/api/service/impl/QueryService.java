package dev.maltsev.money.transfer.api.service.impl;

import dev.maltsev.money.transfer.api.dao.TransactionDao;
import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.service.AbstractService;
import dev.maltsev.money.transfer.api.service.exception.UnknownTransactionException;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.util.List;

/**
 * Read-only operations for transactions
 */
public class QueryService extends AbstractService {

    public QueryService(Sql2o sql) {
        super(sql);
    }

    /**
     * Returns the status of the transaction.
     *
     * @param transactionId the ID of the transaction
     * @param payer         the login of the customer who initiated the transaction
     * @return the status of the transaction
     * @throws UnknownTransactionException if the transaction with the specified ID and payer doesn't exist
     */
    public TransactionStatus getTransactionStatus(String transactionId, String payer) throws UnknownTransactionException {
        try (Connection connection = sql.open()) {
            TransactionStatus status = TransactionDao.getTransactionStatus(transactionId, payer, connection);
            if (status == null) {
                String message = "Transaction with id = '%s' and payer = '%s' not found".formatted(transactionId, payer);
                logger().info(message);
                throw new UnknownTransactionException(message);
            } else {
                return status == TransactionStatus.AWAITING ? TransactionStatus.PROCESSING : status;
            }
        }
    }

    /**
     * Returns all transactions that are stuck in the PROCESSING state.
     *
     * @return the list of stuck transactions
     */
    public List<Transaction> getAllStuckTransactions() {
        try (Connection connection = sql.open()) {
            return TransactionDao.getAllStuckTransactions(connection);
        }
    }
}
