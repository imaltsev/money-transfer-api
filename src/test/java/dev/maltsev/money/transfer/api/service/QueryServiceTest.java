package dev.maltsev.money.transfer.api.service;


import dev.maltsev.money.transfer.api.domain.entity.Account;
import dev.maltsev.money.transfer.api.domain.entity.Customer;
import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.json.TransferRequest;
import dev.maltsev.money.transfer.api.domain.json.WithdrawRequest;
import dev.maltsev.money.transfer.api.domain.object.Money;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.service.exception.UnknownTransactionException;
import dev.maltsev.money.transfer.api.service.impl.QueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class QueryServiceTest extends AbstractDatabaseTest {

    private static QueryService queryService = new QueryService(sql2o);

    @AfterEach
    public void tearDown() {
        cleanupDatabase();
    }

    @Test
    public void testGetTransactionStatus_Ok() throws UnknownTransactionException {
        // Arrange
        TransferRequest request = new TransferRequest(UUID.randomUUID(), "credit", "debit", "login", Money.fromInt(100));
        Customer customer = new Customer("login").addAccount(new Account("credit", Money.fromInt(200))).addAccount(new Account("debit", Money.fromInt(100)));
        Transaction transaction = request.toTransaction(customer.login());
        arrangeCustomer(customer);
        arrangeTransaction(transaction);

        // Act
        TransactionStatus status = queryService.getTransactionStatus(transaction.id(), customer.login());

        // Assert
        assertEquals(TransactionStatus.PROCESSING, status);
    }

    @Test
    public void testGetTransactionStatus_UnknownTransaction_Fail() {
        // Arrange
        String nonExistentTransactionId = UUID.randomUUID().toString();

        // Act & Assert
        assertThrows(UnknownTransactionException.class, () -> queryService.getTransactionStatus(nonExistentTransactionId, "login"));
    }

    @Test
    public void testGetAllStuckTransactions_TransferRequest_Ok() {
        // Arrange
        Customer customer = new Customer("login").addAccount(new Account("credit", Money.fromInt(200))).addAccount(new Account("debit", Money.fromInt(100)));
        arrangeCustomer(customer);

        TransferRequest transferRequest = new TransferRequest(UUID.randomUUID(), "credit", "debit", "login", Money.fromInt(100));
        Transaction transaction = transferRequest.toTransaction(customer.login());
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setUpdated(transaction.created().minusSeconds(61));
        arrangeTransaction(transaction);
        // Act
        List<Transaction> stuckTransactions = queryService.getAllStuckTransactions();

        // Assert
        assertNotNull(stuckTransactions);
        assertEquals(1, stuckTransactions.size());
        assertEquals(transaction.id(), stuckTransactions.get(0).id());
    }

    @Test
    public void testGetAllStuckTransactions_WithdrawRequest_Ok() {
        // Arrange
        Customer customer = new Customer("login").addAccount(new Account("credit", Money.fromInt(200))).addAccount(new Account("debit", Money.fromInt(100)));
        arrangeCustomer(customer);

        WithdrawRequest withdrawRequest = new WithdrawRequest(UUID.randomUUID(), "credit", "http://localhost:8090", Money.fromInt(100));
        Transaction transaction = withdrawRequest.toTransaction(customer.login());
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setUpdated(transaction.created().minusSeconds(61));
        arrangeTransaction(transaction);

        // Act
        List<Transaction> stuckTransactions = queryService.getAllStuckTransactions();

        // Assert
        assertNotNull(stuckTransactions);
        assertEquals(1, stuckTransactions.size());
        assertEquals(transaction.id(), stuckTransactions.get(0).id());
    }

    @Test
    public void testGetAllStuckTransactions_Negative() {
        // Arrange
        Customer customer = new Customer("login").addAccount(new Account("credit", Money.fromInt(200))).addAccount(new Account("debit", Money.fromInt(100)));
        arrangeCustomer(customer);

        TransferRequest transferRequest1 = new TransferRequest(UUID.randomUUID(), "credit", "debit", "login", Money.fromInt(100));
        Transaction transaction2 = transferRequest1.toTransaction(customer.login());
        transaction2.setStatus(TransactionStatus.AWAITING);
        transaction2.setUpdated(transaction2.created().minusSeconds(61));
        arrangeTransaction(transaction2);

        TransferRequest transferRequest2 = new TransferRequest(UUID.randomUUID(), "credit", "debit", "login", Money.fromInt(100));
        Transaction transaction3 = transferRequest2.toTransaction(customer.login());
        transaction3.setStatus(TransactionStatus.COMPLETED);
        transaction3.setUpdated(transaction3.created().minusSeconds(61));
        arrangeTransaction(transaction3);

        TransferRequest transferRequest3 = new TransferRequest(UUID.randomUUID(), "credit", "debit", "login", Money.fromInt(100));
        Transaction transaction4 = transferRequest3.toTransaction(customer.login());
        transaction4.setStatus(TransactionStatus.FAILED);
        transaction4.setUpdated(transaction4.created().minusSeconds(61));
        arrangeTransaction(transaction4);

        // Act
        List<Transaction> stuckTransactions = queryService.getAllStuckTransactions();

        // Assert
        assertNotNull(stuckTransactions);
        assertEquals(0, stuckTransactions.size());
    }
}
