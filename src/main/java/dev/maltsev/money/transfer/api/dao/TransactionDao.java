package dev.maltsev.money.transfer.api.dao;

import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.object.Money;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.domain.object.TransactionType;
import dev.maltsev.money.transfer.api.logging.Loggable;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.ResultSetHandler;
import org.sql2o.Sql2oException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static dev.maltsev.money.transfer.api.dao.TransactionHistoryDao.insertTransactionHistory;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class TransactionDao implements Loggable {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionDao.class);

    public static String findTransactionIdByRequestIdAndPayer(String requestId, String payer, Connection connection) {
        return connection.createQuery("SELECT ID FROM TRANSACTIONS WHERE REQUEST_ID = :requestId AND PAYER = :payer")
                .addParameter("requestId", requestId)
                .addParameter("payer", payer)
                .executeAndFetchFirst((ResultSetHandler<String>) resultSet -> resultSet.getString("ID"));
    }

    public static List<Transaction> getAllTransactions(Connection connection) {
        return connection.createQuery("SELECT * FROM TRANSACTIONS")
                .executeAndFetch(TransactionDao::toTransaction);
    }

    public static Transaction findTransactionById(String id, Connection connection) {
        return connection.createQuery("SELECT * FROM TRANSACTIONS WHERE ID = :id")
                .addParameter("id", id)
                .executeAndFetchFirst(TransactionDao::toTransaction);
    }

    public static String tryInsertTransaction(Transaction transaction, Connection conn) throws TransactionAlreadyExistsException {
        try {
            insertTransaction(transaction, conn);
        } catch (Sql2oException e) {
            if (DaoUtils.isUniqueConstraintViolation(e)) {
                LOG.info("Transaction with requestId = '{}' and customerLogin = '{}' already exists", transaction.requestId(), transaction.payer());
                throw new TransactionAlreadyExistsException();
            } else {
                throw e;
            }
        }
        insertTransactionHistory(transaction, conn);
        return transaction.id();
    }

    private static void insertTransaction(Transaction transaction, Connection connection) {
        connection.createQuery("INSERT INTO TRANSACTIONS (ID, REQUEST_ID, PAYER, PAYER_ACCOUNT_NUMBER, RECIPIENT, RECIPIENT_ACCOUNT_NUMBER, " +
                        "WITHDRAWAL_ADDRESS, AMOUNT, TYPE, STATUS, ERROR_MESSAGE, CREATED, UPDATED) " +
                        "VALUES (:id, :requestId, :payer, :payerAccountNumber, :recipient, :recipientAccountNumber, :withdrawalAddress, :amount, :type, :status, " +
                        ":errorMessage, :created, :updated)")
                .addParameter("id", transaction.id())
                .addParameter("requestId", transaction.requestId())
                .addParameter("payer", transaction.payer())
                .addParameter("payerAccountNumber", transaction.payerAccountNumber())
                .addParameter("recipient", transaction.recipient())
                .addParameter("recipientAccountNumber", transaction.recipientAccountNumber())
                .addParameter("withdrawalAddress", transaction.withdrawalAddress())
                .addParameter("amount", transaction.amount().value())
                .addParameter("type", transaction.type())
                .addParameter("status", transaction.status())
                .addParameter("errorMessage", transaction.errorMessage())
                .addParameter("created", transaction.created())
                .addParameter("updated", transaction.updated())
                .executeUpdate();
    }

    public static Transaction lockTransactionById(String transactionId, Connection connection) {
        return connection.createQuery("SELECT * FROM TRANSACTIONS WHERE id = :transactionId FOR UPDATE")
                .addParameter("transactionId", transactionId)
                .executeAndFetchFirst(TransactionDao::toTransaction);
    }

    public static TransactionStatus getTransactionStatus(String transactionId, String payer, Connection connection) {
        return connection.createQuery("SELECT STATUS FROM TRANSACTIONS WHERE ID = :transactionId AND PAYER = :payer")
                .addParameter("transactionId", transactionId)
                .addParameter("payer", payer)
                .executeAndFetchFirst(TransactionStatus.class);
    }

    private static Transaction toTransaction(ResultSet resultSet) throws SQLException {
        return new Transaction(
                resultSet.getString("id"),
                resultSet.getString("request_id"),
                resultSet.getString("payer"),
                resultSet.getString("payer_account_number"),
                resultSet.getString("recipient"),
                resultSet.getString("recipient_account_number"),
                resultSet.getString("withdrawal_address"),
                new Money(resultSet.getBigDecimal("amount")),
                TransactionType.valueOf(resultSet.getString("type")),
                TransactionStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("error_message"),
                resultSet.getTimestamp("created").toLocalDateTime(),
                resultSet.getTimestamp("updated").toLocalDateTime()
        );
    }

    public static List<String> findAllStuckTransactionIdsByType(TransactionType transactionType, Connection connection) {
        return connection.createQuery("SELECT ID FROM TRANSACTIONS WHERE TYPE = :transactionType AND STATUS = 'PROCESSING'" +
                        "AND UPDATED < :timestamp")
                .addParameter("transactionType", transactionType)
                .addParameter("timestamp", LocalDateTime.now().minusMinutes(1))
                .executeAndFetch((ResultSetHandler<String>) resultSet -> resultSet.getString("id"));
    }

    public static void updateTransaction(Transaction transaction, Connection connection) {
        connection.createQuery("UPDATE TRANSACTIONS SET STATUS = :status, ERROR_MESSAGE = :errorMessage, UPDATED = :updated WHERE ID = :id")
                .addParameter("id", transaction.id())
                .addParameter("status", transaction.status())
                .addParameter("errorMessage", transaction.errorMessage())
                .addParameter("updated", transaction.updated())
                .executeUpdate();
        insertTransactionHistory(transaction, connection);
    }

    public static class TransactionAlreadyExistsException extends Exception {
    }
}
