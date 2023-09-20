package dev.maltsev.money.transfer.api.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maltsev.money.transfer.api.domain.json.TransferRequest;
import dev.maltsev.money.transfer.api.domain.object.Money;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TransferRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDeserialization_Ok() throws IOException {
        String json = "{"
                + "\"requestId\":\"f47ac10b-58cc-4372-a567-0e02b2c3d479\","
                + "\"payerAccountNumber\":\"account1\","
                + "\"recipientAccountNumber\":\"account2\","
                + "\"recipient\":\"recipient\","
                + "\"amount\":100"
                + "}";

        TransferRequest request = objectMapper.readValue(json, TransferRequest.class);

        assertEquals(UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479"), request.requestId());
        assertEquals("account1", request.payerAccountNumber());
        assertEquals("account2", request.recipientAccountNumber());
        assertEquals("recipient", request.recipient());
        assertEquals(Money.fromInt(100), request.amount());
    }

    @Test
    public void testDeserialization_MissingField_Fail() {
        String json = "{"
                + "\"requestId\":\"f47ac10b-58cc-4372-a567-0e02b2c3d479\","
                + "\"payerAccountNumber\":\"account1\","
                + "\"amount\":100"
                + "}";

        assertThrows(IOException.class, () -> objectMapper.readValue(json, TransferRequest.class));
    }
}
