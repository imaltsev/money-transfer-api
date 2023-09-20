package dev.maltsev.money.transfer.api.service;

import dev.maltsev.money.transfer.api.dao.DaoUtils;
import dev.maltsev.money.transfer.api.dao.TransactionDao;
import dev.maltsev.money.transfer.api.domain.entity.Account;
import dev.maltsev.money.transfer.api.domain.entity.Customer;
import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.entity.TransactionHistory;
import dev.maltsev.money.transfer.api.domain.json.TransferRequest;
import dev.maltsev.money.transfer.api.domain.json.WithdrawRequest;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.domain.object.TransactionType;
import dev.maltsev.money.transfer.api.logging.Loggable;
import lombok.SneakyThrows;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.io.InputStreamReader;
import java.util.List;

import static dev.maltsev.money.transfer.api.dao.AccountDao.getAccountByNumber;
import static dev.maltsev.money.transfer.api.dao.CustomerDao.insertCustomerWithAccounts;
import static dev.maltsev.money.transfer.api.dao.TransactionDao.findTransactionById;
import static dev.maltsev.money.transfer.api.dao.TransactionDao.getAllTransactions;
import static dev.maltsev.money.transfer.api.dao.TransactionHistoryDao.findAllTransactionHistoriesById;
import static dev.maltsev.money.transfer.api.dao.TransactionHistoryDao.getAllTransactionHistories;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractDatabaseTest implements Loggable {
    protected static Sql2o sql2o = DaoUtils.setupDatabase();

    public static void arrangeCustomer(Customer customer) {
        try (Connection connection = sql2o.open()) {
            insertCustomerWithAccounts(customer, connection);
        }
    }

    public static void assertCustomerAccounts(Customer customer) {
        try (Connection connection = sql2o.open()) {
            customer.accounts().forEach(account -> {
                Account actualAccount = getAccountByNumber(account.number(), connection);
                assertNotNull(actualAccount);
                assertEquals(account.balance(), actualAccount.balance());
            });
        }
    }

    @SneakyThrows
    public static void arrangeTransaction(Transaction transaction) {
        try (Connection connection = sql2o.open()) {
            TransactionDao.tryInsertTransaction(transaction, connection);
        }
    }

    @SneakyThrows
    public static void cleanupDatabase() {
        try (Connection connection = sql2o.open()) {
            ScriptRunner runner = new ScriptRunner(connection.getJdbcConnection());
            runner.runScript(new InputStreamReader(AbstractDatabaseTest.class.getResourceAsStream("/sql/cleanup.sql")));
        }
    }

    public static void assertNewTransaction(String transactionId, Customer customer, TransferRequest request) {
        try (Connection connection = sql2o.open()) {
            List<Transaction> transactions = getAllTransactions(connection);
            assertEquals(1, transactions.size());
            Transaction transaction = findTransactionById(transactionId, connection);
            assertNotNull(transaction);
            assertNotNull(transaction.id());
            assertEquals(request.requestId().toString(), transaction.requestId());
            assertEquals(customer.login(), transaction.payer());
            assertTrue(customer.accounts().stream().anyMatch(account -> account.number().equals(transaction.recipientAccountNumber())));
            assertEquals(customer.login(), transaction.recipient());
            assertTrue(customer.accounts().stream().anyMatch(account -> account.number().equals(transaction.payerAccountNumber())));
            assertEquals(request.amount(), transaction.amount());
            assertEquals(TransactionType.TRANSFER, transaction.type());
            assertEquals(TransactionStatus.PROCESSING, transaction.status());
            assertNull(transaction.errorMessage());
            assertNotNull(transaction.created());
            assertNotNull(transaction.updated());
        }
    }

    public static void assertNewTransaction(String transactionId, Customer customer, WithdrawRequest request) {
        try (Connection connection = sql2o.open()) {
            List<Transaction> transactions = getAllTransactions(connection);
            assertEquals(1, transactions.size());
            Transaction transaction = findTransactionById(transactionId, connection);
            assertNotNull(transaction);
            assertNotNull(transaction.id());
            assertEquals(request.requestId().toString(), transaction.requestId());
            assertEquals(customer.login(), transaction.payer());
            assertTrue(customer.accounts().stream().anyMatch(account -> account.number().equals(transaction.payerAccountNumber())));
            assertNull(transaction.recipient());
            assertNull(transaction.recipientAccountNumber());
            assertEquals(request.amount(), transaction.amount());
            assertEquals(request.address(), transaction.withdrawalAddress());
            assertEquals(TransactionType.WITHDRAWAL, transaction.type());
            assertEquals(TransactionStatus.PROCESSING, transaction.status());
            assertNull(transaction.errorMessage());
            assertNotNull(transaction.created());
            assertNotNull(transaction.updated());
        }
    }

    public static void assertTransactionHistory(String transactionId, TransactionStatus status) {
        try (Connection connection = sql2o.open()) {
            List<TransactionHistory> transactionHistories = getAllTransactionHistories(connection);
            assertEquals(1, transactionHistories.size());
            transactionHistories = findAllTransactionHistoriesById(transactionId, connection);
            assertEquals(1, transactionHistories.size());

            TransactionHistory transactionHistory = transactionHistories.get(0);
            assertNotNull(transactionHistory);
            assertNotNull(transactionHistory.id());
            assertEquals(status, transactionHistory.status());
            assertNotNull(transactionHistory.timestamp());
        }
    }
}
