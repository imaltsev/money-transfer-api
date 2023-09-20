package dev.maltsev.money.transfer.api.service;

import dev.maltsev.money.transfer.api.domain.entity.Account;
import dev.maltsev.money.transfer.api.domain.entity.Customer;
import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.json.WithdrawRequest;
import dev.maltsev.money.transfer.api.domain.object.Money;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.service.impl.WithdrawCommandService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WithdrawCommandServiceTest extends AbstractDatabaseTest {

    private static WithdrawCommandService withdrawCommandService = new WithdrawCommandService(sql2o);

    @AfterEach
    public void tearDown() {
        cleanupDatabase();
    }

    @Test
    public void testCreateTransaction_Ok() {
        // Arrange
        WithdrawRequest request = new WithdrawRequest(UUID.randomUUID(), "credit", "http://localhost:8090", Money.fromInt(100));
        Customer customer = new Customer("login").addAccount(new Account("credit", Money.fromInt(200)));
        Transaction transaction = request.toTransaction(customer.login());
        arrangeCustomer(customer);

        // Act
        String transactionId = withdrawCommandService.createTransaction(transaction);

        // Assert
        assertNotNull(transactionId);
        assertNewTransaction(transactionId, customer, request);
        assertTransactionHistory(transactionId, TransactionStatus.PROCESSING);
    }

    @Test
    public void testCreateTransaction_serialSameTransactions_Ok() {
        // Arrange
        WithdrawRequest request = new WithdrawRequest(UUID.randomUUID(), "credit", "http://localhost:8090", Money.fromInt(100));
        Customer customer = new Customer("login").addAccount(new Account("credit", Money.fromInt(200)));
        Transaction transaction = request.toTransaction(customer.login());
        arrangeCustomer(customer);

        // Act

        String transactionId1 = withdrawCommandService.createTransaction(transaction);
        String transactionId2 = withdrawCommandService.createTransaction(transaction);

        // Assert
        assertNotNull(transactionId1);
        assertNotNull(transactionId2);
        assertEquals(transactionId1, transactionId2);
        assertNewTransaction(transactionId2, customer, request);
        assertTransactionHistory(transactionId1, TransactionStatus.PROCESSING);
    }

    @Test
    public void testCreateTransaction_parallelSameTransactions_Ok() throws InterruptedException, ExecutionException {
        // Arrange
        WithdrawRequest request = new WithdrawRequest(UUID.randomUUID(), "credit", "http://localhost:8090", Money.fromInt(100));
        Customer customer = new Customer("login").addAccount(new Account("credit", Money.fromInt(200)));
        Transaction transaction = request.toTransaction(customer.login());
        arrangeCustomer(customer);

        // Create a thread pool with 10 threads
        try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {
            List<Future<String>> futures = new ArrayList<>();

            // Act
            for (int i = 0; i < 10; i++) {
                futures.add(executorService.submit(() -> withdrawCommandService.createTransaction(transaction)));
            }

            // Get the results
            Set<String> transactionIds = new HashSet<>();
            for (Future<String> future : futures) {
                transactionIds.add(future.get());
            }

            // Assert
            assertEquals(1, transactionIds.size());

            // Shut down the executor service
            executorService.shutdown();
        }
    }

    @Test
    public void testExecuteTransaction_Ok() {
        // Arrange
        WithdrawRequest request = new WithdrawRequest(UUID.randomUUID(), "credit", "http://localhost:8090", Money.fromInt(100));
        Customer customer = new Customer("login").addAccount(new Account("credit", Money.fromInt(200)));
        Transaction transaction = request.toTransaction(customer.login());
        arrangeCustomer(customer);
        arrangeTransaction(transaction);

        // Act
        TransactionStatus status = withdrawCommandService.executeTransaction(transaction.id());

        // Assert
        assertEquals(TransactionStatus.AWAITING, status);
    }

    @Test
    public void testExecuteTransaction_InsufficientFunds_Fail() {
        // Arrange
        WithdrawRequest request = new WithdrawRequest(UUID.randomUUID(), "credit", "http://localhost:8090", Money.fromInt(300));
        Customer customer = new Customer("login").addAccount(new Account("credit", Money.fromInt(200)));
        Transaction transaction = request.toTransaction(customer.login());
        arrangeCustomer(customer);
        arrangeTransaction(transaction);

        // Act
        TransactionStatus status = withdrawCommandService.executeTransaction(transaction.id());

        // Assert
        assertEquals(TransactionStatus.FAILED, status);
    }
}
