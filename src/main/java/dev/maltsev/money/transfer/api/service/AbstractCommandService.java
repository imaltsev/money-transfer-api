package dev.maltsev.money.transfer.api.service;

import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import static dev.maltsev.money.transfer.api.dao.TransactionDao.*;

public abstract class AbstractCommandService extends AbstractService {

    public AbstractCommandService(Sql2o sql) {
        super(sql);
    }

    protected String insertTransaction(Transaction transaction) {
        try (Connection connection = sql.beginTransaction()) {
            String transactionId = findTransactionIdByRequestIdAndPayer(transaction.requestId(), transaction.payer(), connection);
            if (transactionId == null) {
                try {
                    transactionId = tryInsertTransaction(transaction, connection);
                    connection.commit();
                } catch (TransactionAlreadyExistsException e) {
                    logTransactionAlreadyExist(transaction);
                    transactionId = findTransactionIdByRequestIdAndPayer(transaction.requestId(), transaction.payer(), connection);
                }
            } else {
                logTransactionAlreadyExist(transaction);
            }
            return transactionId;
        }
    }

    private void logTransactionAlreadyExist(Transaction transaction) {
        logger().info("Transaction with requestId = '{}' and customerLogin = '{}' already exists", transaction.requestId(), transaction.payer());
    }
}
