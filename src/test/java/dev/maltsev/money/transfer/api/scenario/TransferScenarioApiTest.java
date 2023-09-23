package dev.maltsev.money.transfer.api.scenario;

import dev.maltsev.money.transfer.api.Application;
import dev.maltsev.money.transfer.api.domain.object.Money;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.hamcrest.text.MatchesPattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class TransferScenarioApiTest extends AbstractScenarioApiTest {

    @BeforeAll
    public static void startServer() {
        Application.main(new String[]{"--recoveryInterval", "100"});
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
    }

    private static String assertSendTransfer(String customer, String request) {
        return
                given()
                        .contentType(ContentType.JSON)
                        .body(request).
                        when()
                        .post("customers/%s/transfer" .formatted(customer)).
                        then()
                        .statusCode(200)
                        .body("transactionId", MatchesPattern.matchesPattern("[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}"))
                        .extract()
                        .path("transactionId");
    }

    @AfterEach
    public void tearDown() {
        cleanupDatabase();
    }

    @Test
    public void testTransfer_NoBody_Fail() {
        given()
                .contentType(ContentType.JSON).
                when()
                .post("customers/customer/transfer").
                then()
                .statusCode(400)
                .body("message", Matchers.equalTo("Request body is required"));
    }

    @Test
    public void testTransfer_EmptyBody_Fail() {
        given()
                .contentType(ContentType.JSON)
                .body("").
                when()
                .post("customers/customer/transfer").
                then()
                .statusCode(400)
                .body("message", Matchers.equalTo("Request body is required"));
    }

    @Test
    public void testTransfer_InvalidJsonBody_Fail() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"key\": \"value\"}").
                when()
                .post("customers/customer/transfer").
                then()
                .statusCode(400)
                .body("message", Matchers.containsString("requestId can't be null"));
    }

    @Test
    public void testTransfer_SameAccounts_Fail() {
        given()
                .contentType(ContentType.JSON)
                .body(getJsonRequest("transfer/same-accounts")).
                when()
                .post("customers/customer/transfer").
                then()
                .statusCode(400)
                .body("message", Matchers.containsString("payerAccountNumber and recipientAccountNumber can't be the same"));
    }

    @Test
    public void testTransfer_NegativeAmount_Fail() {
        given()
                .contentType(ContentType.JSON)
                .body(getJsonRequest("transfer/negative-amount")).
                when()
                .post("customers/customer/transfer").
                then()
                .statusCode(400)
                .body("message", Matchers.containsString("amount can't be zero or negative"));
    }

    @Test
    public void testTransfer_ZeroAmount_Fail() {
        given()
                .contentType(ContentType.JSON)
                .body(getJsonRequest("transfer/zero-amount")).
                when()
                .post("customers/customer/transfer").
                then()
                .statusCode(400)
                .body("message", Matchers.containsString("amount can't be zero or negative"));
    }

    @Test
    public void testTransfer_SingleCustomer_Ok() {
        arrangeDatabase("transfer/single-customer");

        String transactionId = assertSendTransfer("customer", getJsonRequest("transfer/single-customer"));
        TransactionStatus status = assertGetTransactionStatus("customer", transactionId);

        assertEquals(TransactionStatus.COMPLETED, status);
        assertPayerAccountBalance(transactionId, Money.fromInt(100));
        assertRecipientAccountBalance(transactionId, Money.fromInt(200));
    }

    @Test
    public void testTransfer_TwoCustomers_Ok() {
        arrangeDatabase("transfer/two-customers");

        String transactionId = assertSendTransfer("customer1", getJsonRequest("transfer/two-customers"));
        TransactionStatus status = assertGetTransactionStatus("customer1", transactionId);

        assertEquals(TransactionStatus.COMPLETED, status);
        assertPayerAccountBalance(transactionId, Money.fromInt(100));
        assertRecipientAccountBalance(transactionId, Money.fromInt(200));
    }

    @Test
    public void testTransfer_InsufficientFunds_Fail() {
        arrangeDatabase("transfer/insufficient-funds");

        String transactionId = assertSendTransfer("customer1", getJsonRequest("transfer/insufficient-funds"));
        TransactionStatus status = assertGetTransactionStatus("customer1", transactionId);

        assertEquals(TransactionStatus.FAILED, status);
        assertPayerAccountBalance(transactionId, Money.fromInt(50));
        assertRecipientAccountBalance(transactionId, Money.fromInt(100));
    }

    @Test
    public void testTransfer_UnknownTransactionId_Fail() {
        arrangeDatabase("transfer/single-customer");
        assertSendTransfer("customer", getJsonRequest("transfer/single-customer"));

        String status = given()
                .when()
                .get("/customers/%s/transactions/%s/status" .formatted("customer", UUID.randomUUID().toString()))
                .then()
                .statusCode(400)
                .extract()
                .path("status");

        assertNull(status);
    }

    @Test
    public void testTransfer_UnknownCustomer_Fail() {
        arrangeDatabase("transfer/single-customer");

        String transactionId = assertSendTransfer("customer", getJsonRequest("transfer/single-customer"));
        String status = given()
                .when()
                .get("/customers/%s/transactions/%s/status" .formatted("unknown_customer", transactionId))
                .then()
                .statusCode(400)
                .extract()
                .path("status");

        assertNull(status);
    }

    @Test
    public void testTransfer_StuckTransactionRecovered_Ok() {
        arrangeDatabase("transfer/stuck");
        String transactionId = "b3a3f6e1-a7d9-4a2f-8c76-74d01a3a17e1";

        TransactionStatus status = assertGetTransactionStatus("customer1", transactionId);

        assertEquals(TransactionStatus.COMPLETED, status);
        assertPayerAccountBalance(transactionId, Money.fromInt(100));
        assertRecipientAccountBalance(transactionId, Money.fromInt(200));
    }

    @Test
    public void testTransfer_SameTransferSerial_Ok() {
        arrangeDatabase("transfer/two-customers");

        Set<String> transactionIds = IntStream.rangeClosed(1, 6).mapToObj(value ->
                assertSendTransfer("customer1", getJsonRequest("transfer/two-customers"))
        ).collect(Collectors.toSet());

        assertNotNull(transactionIds);
        assertEquals(1, transactionIds.size());

        String transactionId = transactionIds.iterator().next();

        TransactionStatus status = assertGetTransactionStatus("customer1", transactionId);

        assertEquals(TransactionStatus.COMPLETED, status);
        assertPayerAccountBalance(transactionId, Money.fromInt(100));
        assertRecipientAccountBalance(transactionId, Money.fromInt(200));
    }

    @SneakyThrows
    @Test
    public void testTransfer_SameTransactionsParallel_Ok() {
        arrangeDatabase("transfer/two-customers");

        try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {
            List<Future<String>> futures = new ArrayList<>();

            // Act
            for (int i = 0; i < 10; i++) {
                futures.add(executorService.submit(() -> assertSendTransfer("customer1", getJsonRequest("transfer/two-customers"))));
            }

            // Get the results
            Set<String> transactionIds = new HashSet<>();
            for (Future<String> future : futures) {
                transactionIds.add(future.get());
            }

            // Assert
            assertEquals(1, transactionIds.size());
            String transactionId = transactionIds.iterator().next();
            TransactionStatus status = assertGetTransactionStatus("customer1", transactionId);

            assertEquals(TransactionStatus.COMPLETED, status);
            assertPayerAccountBalance(transactionId, Money.fromInt(100));
            assertRecipientAccountBalance(transactionId, Money.fromInt(200));

            executorService.shutdown();
        }
    }
}