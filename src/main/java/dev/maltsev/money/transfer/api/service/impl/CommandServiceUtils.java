package dev.maltsev.money.transfer.api.service.impl;

import dev.maltsev.money.transfer.api.domain.entity.Account;
import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.service.exception.InvalidTransactionException;
import lombok.NoArgsConstructor;
import org.sql2o.Connection;

import static dev.maltsev.money.transfer.api.dao.AccountDao.getAccountByNumber;
import static dev.maltsev.money.transfer.api.dao.CustomerDao.getAccountOwner;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class CommandServiceUtils {
    protected static void validateTransaction(Transaction transaction, Connection connection) throws InvalidTransactionException {
        Account payerAccount = getAccountByNumber(transaction.payerAccountNumber(), connection);
        String payerAccountOwner = getAccountOwner(transaction.payerAccountNumber(), connection);

        if (payerAccount == null || !transaction.payer().equals(payerAccountOwner)) {
            throw new InvalidTransactionException(
                    String.format("A pair of account with number '%s' and customer with login '%s' is not found", transaction.payerAccountNumber(),
                            transaction.payer()));
        }

        if (payerAccount.balance().compareTo(transaction.amount()) < 0) {
            throw new InvalidTransactionException(String.format("Insufficient funds in account '%s'", transaction.payerAccountNumber()));
        }
    }

    static void validateTransferTransaction(Transaction transaction, Connection connection) throws InvalidTransactionException {
        validateTransaction(transaction, connection);

        if (transaction.payerAccountNumber().equalsIgnoreCase(transaction.recipientAccountNumber())) {
            throw new InvalidTransactionException(
                    String.format("Payer account '%s' and recipient account '%s' can't be the same", transaction.payerAccountNumber(),
                            transaction.recipientAccountNumber()));
        }

        Account recipientAccount = getAccountByNumber(transaction.recipientAccountNumber(), connection);
        String recipientAccountOwner = getAccountOwner(transaction.recipientAccountNumber(), connection);

        if (recipientAccount == null || !transaction.recipient().equals(recipientAccountOwner)) {
            throw new InvalidTransactionException(
                    String.format("A pair of account with number '%s' and customer with login '%s' is not found", transaction.payerAccountNumber(),
                            transaction.payer()));
        }
    }
}
