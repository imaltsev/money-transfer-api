package dev.maltsev.money.transfer.api.dao;

import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.entity.Account;
import dev.maltsev.money.transfer.api.domain.object.TransactionType;
import lombok.NoArgsConstructor;
import org.sql2o.Connection;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class AccountDao {

    public static void insertAccount(Account account, Connection connection) {
        connection.createQuery("INSERT INTO ACCOUNTS (NUMBER, BALANCE, CURRENCY) VALUES (:number, :balance, :currency)")
                .addParameter("number", account.number())
                .addParameter("balance", account.balance().value())
                .addParameter("currency", account.currency())
                .executeUpdate();
    }

    public static Account toAccount(ResultSet resultSet) throws SQLException {
        String number = resultSet.getString("number");
        BigDecimal balance = resultSet.getBigDecimal("balance");
        return new Account(number, balance);
    }

    public static void addAmountToRecipientAccount(Transaction transaction, Connection connection) {
        connection.createQuery("UPDATE accounts SET balance = balance + :amount WHERE number = :number")
                .addParameter("amount", transaction.amount().value())
                .addParameter("number", transaction.recipientAccountNumber())
                .executeUpdate();
    }
    public static void refundPayerAccount(Transaction transaction, Connection connection) {
        connection.createQuery("UPDATE accounts SET balance = balance + :amount WHERE number = :number")
                .addParameter("amount", transaction.amount().value())
                .addParameter("number", transaction.payerAccountNumber())
                .executeUpdate();
    }

    public static void lockInvolvedAccounts(Transaction transaction, Connection connection) {
        if (transaction.type() == TransactionType.TRANSFER) {
            connection.createQuery("SELECT * FROM accounts WHERE number IN (:payerAccountNumber, :recipientAccountNumber) FOR UPDATE")
                    .addParameter("payerAccountNumber", transaction.payerAccountNumber())
                    .addParameter("recipientAccountNumber", transaction.recipientAccountNumber())
                    .executeAndFetchTable();
        } else {
            connection.createQuery("SELECT * FROM accounts WHERE number = :number FOR UPDATE")
                    .addParameter("number", transaction.payerAccountNumber())
                    .executeAndFetchTable();
        }
    }

    public static void subtractAmountFromPayerAccount(Transaction transaction, Connection connection) {
        connection.createQuery("UPDATE accounts SET balance = balance - :amount WHERE number = :number")
                .addParameter("amount", transaction.amount().value())
                .addParameter("number", transaction.payerAccountNumber())
                .executeUpdate();
    }

    public static Account getAccountByNumber(String accountNumber, Connection connection) {
        return connection.createQuery("SELECT * FROM accounts WHERE number = :number")
                .addParameter("number", accountNumber)
                .executeAndFetchFirst(AccountDao::toAccount);
    }
}
