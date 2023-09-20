package dev.maltsev.money.transfer.api.dao;

import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.entity.TransactionHistory;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.logging.Loggable;
import lombok.NoArgsConstructor;
import org.sql2o.Connection;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class TransactionHistoryDao implements Loggable {

    public static List<TransactionHistory> findAllTransactionHistoriesById(String id, Connection connection) {
        return connection.createQuery("SELECT * FROM TRANSACTION_HISTORY WHERE ID = :id")
                .addParameter("id", id)
                .executeAndFetch(TransactionHistoryDao::toTransactionHistory);
    }

    public static List<TransactionHistory> getAllTransactionHistories(Connection connection) {
        return connection.createQuery("SELECT * FROM TRANSACTION_HISTORY")
                .executeAndFetch(TransactionHistoryDao::toTransactionHistory);
    }

    public static void insertTransactionHistory(Transaction transaction, Connection connection) {
        connection.createQuery("INSERT INTO TRANSACTION_HISTORY (ID, STATUS, TIMESTAMP) VALUES (:id, :status, :timestamp)")
                .addParameter("id", transaction.id())
                .addParameter("status", transaction.status())
                .addParameter("timestamp", transaction.updated())
                .executeUpdate();
    }

    private static TransactionHistory toTransactionHistory(ResultSet resultSet) throws SQLException {
        return new TransactionHistory(
                resultSet.getString("ID"),
                TransactionStatus.valueOf(resultSet.getString("STATUS")),
                resultSet.getTimestamp("TIMESTAMP").toLocalDateTime()
        );
    }
}
