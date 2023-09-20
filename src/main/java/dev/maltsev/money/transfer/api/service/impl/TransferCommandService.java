package dev.maltsev.money.transfer.api.service.impl;

import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.service.AbstractCommandService;
import dev.maltsev.money.transfer.api.service.CommandService;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import static dev.maltsev.money.transfer.api.dao.AccountDao.*;
import static dev.maltsev.money.transfer.api.dao.TransactionDao.lockTransactionById;
import static dev.maltsev.money.transfer.api.dao.TransactionDao.updateTransaction;
import static dev.maltsev.money.transfer.api.domain.object.TransactionStatus.PROCESSING;
import static dev.maltsev.money.transfer.api.service.impl.CommandServiceUtils.validateTransferTransaction;

public class TransferCommandService extends AbstractCommandService implements CommandService {

    public TransferCommandService(Sql2o sql) {
        super(sql);
    }

    @Override
    public String createTransaction(Transaction transaction) {
        return insertTransaction(transaction);
    }

    @Override
    public TransactionStatus executeTransaction(String transactionId) {
        try (Connection connection = sql.beginTransaction()) {
            Transaction transaction = lockTransactionById(transactionId, connection);
            if (transaction != null) {
                if (transaction.status() == PROCESSING) {
                    try {
                        validateTransferTransaction(transaction, connection);
                        lockRowsForInvolvedAccounts(transaction, connection);
                        subtractAmountFromPayerAccount(transaction, connection);
                        addAmountToRecipientAccount(transaction, connection);
                        updateTransaction(transaction.complete(), connection);
                        logger().info("Transfer transaction with id = '{}' is completed", transactionId);
                    } catch (Exception e) {
                        logger().error("Transaction with id = '{}' failed to process", transactionId, e);
                        updateTransaction(transaction.fail(e), connection);
                    }
                    connection.commit();
                } else {
                    logger().info("Transaction with id = '{}' is already processed", transactionId);
                }
                return transaction.status();
            } else {
                logger().error("Transaction with id = '{}' doesn't exist", transactionId);
                return null;
            }
        }
    }
}
