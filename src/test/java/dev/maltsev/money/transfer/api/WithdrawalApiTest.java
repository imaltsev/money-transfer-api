package dev.maltsev.money.transfer.api;

import dev.maltsev.money.transfer.api.domain.object.Money;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.service.AbstractDatabaseTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.hamcrest.text.MatchesPattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static dev.maltsev.money.transfer.api.AbstractApiTest.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WithdrawalApiTest extends AbstractDatabaseTest {

    @BeforeAll
    public static void startServer() {
        Application.main(new String[]{});
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;
    }

    @AfterEach
    public void tearDown() {
        cleanupDatabase();
    }

    private static String assertWithdrawal(String customer, String request) {
        return
                given()
                        .contentType(ContentType.JSON)
                        .body(request).
                        when()
                        .post("customers/%s/withdraw" .formatted(customer)).
                        then()
                        .statusCode(200)
                        .body("transactionId", MatchesPattern.matchesPattern("[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}"))
                        .extract()
                        .path("transactionId");
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
    public void testWithdraw_SingleWithdrawal_Ok() {
        initDatabase("withdraw/single");

        String transactionId = assertWithdrawal("customer", getJsonRequest("withdraw/single"));
        TransactionStatus status = assertGetTransactionStatus("customer", transactionId);

        assertTrue(EnumSet.of(TransactionStatus.COMPLETED, TransactionStatus.FAILED).contains(status));
        if (status == TransactionStatus.COMPLETED) {
            assertPayerAccountBalance(transactionId, Money.fromInt(100));
        }
        if (status == TransactionStatus.FAILED) {
            assertPayerAccountBalance(transactionId, Money.fromInt(200));
        }
    }

    @Test
    public void testWithdraw_InsufficientFunds_Fail() {
        initDatabase("withdraw/insufficient-funds");

        String transactionId = assertWithdrawal("customer", getJsonRequest("withdraw/insufficient-funds"));
        TransactionStatus status = assertGetTransactionStatus("customer", transactionId);

        assertEquals(TransactionStatus.FAILED, status);
        assertPayerAccountBalance(transactionId, Money.fromInt(100));
    }
}