package dev.maltsev.money.transfer.api;

import dev.maltsev.money.transfer.api.dao.TransactionDao;
import dev.maltsev.money.transfer.api.domain.entity.Account;
import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.object.Money;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.service.AbstractDatabaseTest;
import lombok.SneakyThrows;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.sql2o.Connection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import static dev.maltsev.money.transfer.api.dao.AccountDao.getAccountByNumber;
import static io.restassured.RestAssured.given;
import static java.nio.file.Files.readString;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractScenarioApiTest extends AbstractDatabaseTest {

    public static void arrangeDatabase(String folder) {
        try {
            String scriptString = readString(Paths.get("src/test/resources/data/" + folder + "/db.sql"));
            ScriptRunner scriptRunner = new ScriptRunner(sql2o.open().getJdbcConnection());
            scriptRunner.setSendFullScript(true);
            scriptRunner.setStopOnError(true);
            scriptRunner.runScript(new InputStreamReader(new ByteArrayInputStream(scriptString.getBytes())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getJsonRequest(String folder) {
        try {
            return readString(Paths.get("src/test/resources/data/" + folder + "/request.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertPayerAccountBalance(String transactionId, Money balance) {
        try (Connection connection = sql2o.open()) {
            Transaction transaction = TransactionDao.findTransactionById(transactionId, connection);
            assertNotNull(transaction);
            Account account = getAccountByNumber(transaction.payerAccountNumber(), connection);
            assertEquals(balance, account.balance());
        }
    }

    public static void assertRecipientAccountBalance(String transactionId, Money balance) {
        try (Connection connection = sql2o.open()) {
            Transaction transaction = TransactionDao.findTransactionById(transactionId, connection);
            assertNotNull(transaction);
            Account account = getAccountByNumber(transaction.recipientAccountNumber(), connection);
            assertEquals(balance, account.balance());
        }
    }

    @SneakyThrows
    public static TransactionStatus assertGetTransactionStatus(String customer, String transactionId) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        AtomicReference<TransactionStatus> transactionStatus = new AtomicReference<>();
        ScheduledFuture<?> future = executor.scheduleWithFixedDelay(() -> {
            String status = given()
                    .when()
                    .get("/customers/%s/transactions/%s/status" .formatted(customer, transactionId))
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("status");

            if (TransactionStatus.COMPLETED.name().equals(status) || TransactionStatus.FAILED.name().equals(status)) {
                transactionStatus.set(TransactionStatus.valueOf(status));
                executor.shutdown();
            }
        }, 100, 1000, MILLISECONDS);

        executor.awaitTermination(15, SECONDS);
        if (!executor.isShutdown()) {
            future.cancel(true);
            executor.shutdown();
        }

        return transactionStatus.get();
    }
}
