package dev.maltsev.money.transfer.api;

import dev.maltsev.money.transfer.api.domain.object.Money;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.hamcrest.text.MatchesPattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TransferScenarioApiTest extends AbstractScenarioApiTest {

    @BeforeAll
    public static void startServer() {
        System.setProperty("recoveryDelay", "100");
        Application.main(new String[]{});
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
    public void testTransfer_SingleCustomer_Ok() {
        arrangeDatabase("transfer/single-customer");

        String transactionId = assertSendTransfer("customer", getJsonRequest("transfer/single-customer"));
        TransactionStatus status = assertGetTransactionStatus("customer", transactionId);

        assertEquals(TransactionStatus.COMPLETED, status);
        assertPayerAccountBalance(transactionId, Money.fromInt(100));
        assertRecipientAccountBalance(transactionId, Money.fromInt(200));
    }

    @Test
    public void testTransfer_2Customers_Ok() {
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
    public void testTransactionStatus_UnknownTransactionId_Failed() {
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
    public void testTransactionStatus_UnknownCustomer_Failed() {
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
}