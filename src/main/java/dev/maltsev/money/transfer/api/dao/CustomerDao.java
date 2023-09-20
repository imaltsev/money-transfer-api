package dev.maltsev.money.transfer.api.dao;

import dev.maltsev.money.transfer.api.domain.entity.Account;
import dev.maltsev.money.transfer.api.domain.entity.Customer;
import lombok.NoArgsConstructor;
import org.sql2o.Connection;
import org.sql2o.ResultSetHandler;

import static dev.maltsev.money.transfer.api.dao.AccountDao.insertAccount;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class CustomerDao {

    public static void insertCustomerWithAccounts(Customer customer, Connection connection) {
        insertCustomer(customer, connection);
        customer.accounts().forEach(account -> {
            insertAccount(account, connection);
            linkAccountToCustomer(customer, account, connection);
        });
    }

    public static void insertCustomer(Customer customer, Connection connection) {
        connection.createQuery("INSERT INTO customers (login) VALUES (:login)")
                .addParameter("login", customer.login())
                .executeUpdate();
    }

    public static void linkAccountToCustomer(Customer customer, Account account, Connection connection) {
        connection.createQuery("INSERT INTO customer_accounts (customer_login, account_number) VALUES (:login, :number)")
                .addParameter("login", customer.login())
                .addParameter("number", account.number())
                .executeUpdate();
    }

    public static String getAccountOwner(String accountNumber, Connection connection) {
        return connection.createQuery("SELECT customer_login FROM customer_accounts WHERE account_number = :number")
                .addParameter("number", accountNumber)
                .executeAndFetchFirst((ResultSetHandler<String>) resultSet -> resultSet.getString("customer_login"));
    }
}
