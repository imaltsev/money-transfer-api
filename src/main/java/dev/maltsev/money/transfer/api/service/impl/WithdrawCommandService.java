package dev.maltsev.money.transfer.api.service.impl;

import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.service.AbstractCommandService;
import dev.maltsev.money.transfer.api.service.CommandService;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.util.UUID;

import static dev.maltsev.money.transfer.api.dao.AccountDao.*;
import static dev.maltsev.money.transfer.api.dao.AccountDao.refundPayerAccount;
import static dev.maltsev.money.transfer.api.dao.TransactionDao.*;
import static dev.maltsev.money.transfer.api.dao.TransactionDao.updateTransaction;
import static dev.maltsev.money.transfer.api.service.impl.CommandServiceUtils.validateTransaction;
import static dev.maltsev.money.transfer.api.service.impl.WithdrawalService.WithdrawalState.COMPLETED;
import static dev.maltsev.money.transfer.api.service.impl.WithdrawalService.WithdrawalState.FAILED;

public class WithdrawCommandService extends AbstractCommandService implements CommandService {

    private final WithdrawalService externalWithdrawalService = new WithdrawalServiceStub();

    public WithdrawCommandService(Sql2o sql) {
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
                if (transaction.status() == TransactionStatus.PROCESSING) {
                    return processTransaction(transaction, connection);
                } else if (transaction.status() == TransactionStatus.AWAITING) {
                    return tryCompleteTransaction(transaction, connection);
                } else {
                    logger().info("Transaction with id = '{}' is already processed", transactionId);
                    return transaction.status();
                }
            } else {
                logger().error("Transaction with id = '{}' doesn't exist", transactionId);
                return null;
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private TransactionStatus tryCompleteTransaction(Transaction transaction, Connection connection) {
        WithdrawalService.WithdrawalState withdrawalState = null;
        try {
            withdrawalState = externalWithdrawalService.getRequestState(new WithdrawalService.WithdrawalId(UUID.fromString(transaction.id())));
        } catch (Exception e) {
            logger().error("Transaction with id = '{}' failed to process", transaction.id(), e);
        }
        if (withdrawalState == COMPLETED) {
            updateTransaction(transaction.complete(), connection);
            connection.commit();
            return transaction.status();
        } else if (withdrawalState == FAILED) {
            lockRowsForInvolvedAccounts(transaction, connection);
            refundPayerAccount(transaction, connection);
            updateTransaction(transaction.fail(), connection);
            connection.commit();
            return transaction.status();
        } else {
            // do nothing if withdrawal is still in progress or service is unavailable
        }

        return transaction.status();
    }

    private TransactionStatus processTransaction(Transaction transaction, Connection connection) {
        try {
            validateTransaction(transaction, connection);
            lockRowsForInvolvedAccounts(transaction, connection);
            subtractAmountFromPayerAccount(transaction, connection);

            externalWithdrawalService.requestWithdrawal(new WithdrawalService.WithdrawalId(UUID.fromString(transaction.id())),
                    new WithdrawalService.Address(transaction.withdrawalAddress()), transaction.amount().value());
            logger().info("Withdrawal transaction with id = '{}' is sent to address = {}", transaction.id(), transaction.withdrawalAddress());

            updateTransaction(transaction.await(), connection);
            connection.commit();
        } catch (Exception e) {
            logger().error("Transaction with id = '{}' failed to process", transaction.id(), e);

            connection.rollback();
            updateTransactionSeparately(transaction.fail(e));
        }

        return transaction.status();
    }

    private void updateTransactionSeparately(Transaction transaction) {
        try (Connection connection = sql.beginTransaction()) {
            lockTransactionById(transaction.id(), connection);
            updateTransaction(transaction, connection);
            connection.commit();
        }
    }
}
