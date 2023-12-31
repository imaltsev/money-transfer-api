package dev.maltsev.money.transfer.api.scenario;

import dev.maltsev.money.transfer.api.Application;
import dev.maltsev.money.transfer.api.domain.object.Money;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.service.AbstractDatabaseTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.hamcrest.text.MatchesPattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.maltsev.money.transfer.api.domain.object.TransactionStatus.COMPLETED;
import static dev.maltsev.money.transfer.api.domain.object.TransactionStatus.FAILED;
import static dev.maltsev.money.transfer.api.scenario.AbstractScenarioApiTest.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class WithdrawalScenarioApiTest extends AbstractDatabaseTest {

    @BeforeAll
    public static void startServer() {
        Application.main(new String[]{"--recoveryInterval", "100"});
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
    }

    private static String assertWithdrawal(String customer, String request) {
        return
                given()
                        .contentType(ContentType.JSON)
                        .body(request).
                        when()
                        .post("customers/%s/withdraw".formatted(customer)).
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
    public void testWithdraw_NoBody_Fail() {
        given()
                .contentType(ContentType.JSON).
                when()
                .post("customers/customer/withdraw").
                then()
                .statusCode(400)
                .body("message", Matchers.equalTo("Request body is required"));
    }

    @Test
    public void testWithdraw_EmptyBody_Fail() {
        given()
                .contentType(ContentType.JSON)
                .body("").
                when()
                .post("customers/customer/withdraw").
                then()
                .statusCode(400)
                .body("message", Matchers.equalTo("Request body is required"));
    }

    @Test
    public void testWithdraw_InvalidJsonBody_Fail() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"key\": \"value\"}").
                when()
                .post("customers/customer/withdraw").
                then()
                .statusCode(400)
                .body("message", Matchers.containsString("requestId can't be null"));
    }

    @Test
    public void testWithdraw_AddressNotValidUrl_Fail() {
        given()
                .contentType(ContentType.JSON)
                .body(getJsonRequest("withdraw/invalid-address")).
                when()
                .post("customers/customer/withdraw").
                then()
                .statusCode(400)
                .body("message", Matchers.containsString("invalid address"));
    }

    @Test
    public void testWithdraw_NegativeAmount_Fail() {
        given()
                .contentType(ContentType.JSON)
                .body(getJsonRequest("withdraw/negative-amount")).
                when()
                .post("customers/customer/withdraw").
                then()
                .statusCode(400)
                .body("message", Matchers.containsString("amount can't be zero or negative"));
    }

    @Test
    public void testWithdraw_ZeroAmount_Fail() {
        given()
                .contentType(ContentType.JSON)
                .body(getJsonRequest("withdraw/zero-amount")).
                when()
                .post("customers/customer/withdraw").
                then()
                .statusCode(400)
                .body("message", Matchers.containsString("amount can't be zero or negative"));
    }

    @Test
    public void testWithdraw_SingleWithdrawal_Ok() {
        arrangeDatabase("withdraw/single");

        String transactionId = assertWithdrawal("customer", getJsonRequest("withdraw/single"));
        TransactionStatus status = assertGetTransactionStatus("customer", transactionId);

        assertTrue(EnumSet.of(COMPLETED, FAILED).contains(status));
        if (status == COMPLETED) {
            assertPayerAccountBalance(transactionId, Money.fromInt(100));
        }
        if (status == FAILED) {
            assertPayerAccountBalance(transactionId, Money.fromInt(200));
        }
    }

    @Test
    public void testWithdraw_InsufficientFunds_Fail() {
        arrangeDatabase("withdraw/insufficient-funds");

        String transactionId = assertWithdrawal("customer", getJsonRequest("withdraw/insufficient-funds"));
        TransactionStatus status = assertGetTransactionStatus("customer", transactionId);

        assertEquals(FAILED, status);
        assertPayerAccountBalance(transactionId, Money.fromInt(100));
    }

    @Test
    public void testWithdraw_UnknownTransactionId_Fail() {
        arrangeDatabase("withdraw/single");

        assertWithdrawal("customer", getJsonRequest("withdraw/single"));

        String status = given()
                .when()
                .get("/customers/%s/transactions/%s/status".formatted("customer", UUID.randomUUID().toString()))
                .then()
                .statusCode(400)
                .extract()
                .path("status");

        assertNull(status);
    }

    @Test
    public void testWithdraw_UnknownCustomer_Fail() {
        arrangeDatabase("withdraw/single");

        String transactionId = assertWithdrawal("customer", getJsonRequest("withdraw/single"));
        String status = given()
                .when()
                .get("/customers/%s/transactions/%s/status".formatted("unknown_customer", transactionId))
                .then()
                .statusCode(400)
                .extract()
                .path("status");

        assertNull(status);
    }

    @Test
    public void testWithdraw_StuckTransactionRecovered_Ok() {
        arrangeDatabase("withdraw/stuck");
        String transactionId = "b3a3f6e1-a7d9-4a2f-8c76-74d01a3a17e1";

        TransactionStatus status = assertGetTransactionStatus("customer", transactionId);

        assertTrue(EnumSet.of(COMPLETED, FAILED).contains(status));
        if (status == COMPLETED) {
            assertPayerAccountBalance(transactionId, Money.fromInt(100));
        }
        if (status == FAILED) {
            assertPayerAccountBalance(transactionId, Money.fromInt(200));
        }
    }

    @Test
    public void testWithdraw_SameTransferSerial_Ok() {
        arrangeDatabase("withdraw/single");

        Set<String> transactionIds = IntStream.rangeClosed(1, 6).mapToObj(value ->
                assertWithdrawal("customer", getJsonRequest("withdraw/single"))
        ).collect(Collectors.toSet());

        assertNotNull(transactionIds);
        assertEquals(1, transactionIds.size());

        String transactionId = transactionIds.iterator().next();

        TransactionStatus status = assertGetTransactionStatus("customer", transactionId);

        assertTrue(EnumSet.of(COMPLETED, FAILED).contains(status));
        if (status == COMPLETED) {
            assertPayerAccountBalance(transactionId, Money.fromInt(100));
        }
        if (status == FAILED) {
            assertPayerAccountBalance(transactionId, Money.fromInt(200));
        }
    }

    @SneakyThrows
    @Test
    public void testTransfer_SameTransactionsParallel_Ok() {
        arrangeDatabase("withdraw/single");

        try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {
            List<Future<String>> futures = new ArrayList<>();

            // Act
            for (int i = 0; i < 10; i++) {
                futures.add(executorService.submit(() -> assertWithdrawal("customer", getJsonRequest("withdraw/single"))));
            }

            // Get the results
            Set<String> transactionIds = new HashSet<>();
            for (Future<String> future : futures) {
                transactionIds.add(future.get());
            }

            // Assert
            assertEquals(1, transactionIds.size());
            String transactionId = transactionIds.iterator().next();

            TransactionStatus status = assertGetTransactionStatus("customer", transactionId);

            assertTrue(EnumSet.of(COMPLETED, FAILED).contains(status));
            if (status == COMPLETED) {
                assertPayerAccountBalance(transactionId, Money.fromInt(100));
            }
            if (status == FAILED) {
                assertPayerAccountBalance(transactionId, Money.fromInt(200));
            }

            executorService.shutdown();
        }
    }
}