package dev.maltsev.money.transfer.api.verticle;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.maltsev.money.transfer.api.domain.json.TransferRequest;
import dev.maltsev.money.transfer.api.service.exception.InvalidClientRequestException;
import io.vertx.core.AsyncResult;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class HttpServerVerticleUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    @Test
    void testDeserialize() {
        String json =
                """
                        {
                            \"requestId\":\"f47ac10b-58cc-4372-a567-0e02b2c3d479\",
                            \"payerAccountNumber\":\"account1\",
                            \"recipientAccountNumber\":\"account2\",
                            \"recipient\":\"recipient\",
                            \"amount\":100
                        }
                """.stripIndent();

        TransferRequest expected = objectMapper.readValue(json, TransferRequest.class);

        RoutingContext context = mock(RoutingContext.class);

        RequestBody body = mock(RequestBody.class);
        when(body.asJsonObject()).thenReturn(new JsonObject(json));
        when(body.isEmpty()).thenReturn(false);
        when(context.body()).thenReturn(body);

        TransferRequest actual = HttpServerVerticleUtils.deserialize(context, TransferRequest.class);

        assertEquals(expected, actual);
    }

    @Test
    void testDeserializeInvalidJson() {
        RoutingContext context = mock(RoutingContext.class);

        when(context.getBodyAsJson()).thenThrow(new RuntimeException("Invalid JSON"));

        assertThrows(InvalidClientRequestException.class, () -> HttpServerVerticleUtils.deserialize(context, TransferRequest.class));
    }

    @Test
    void testHandleErrorInvalidClientRequestException() {
        RoutingContext context = mock(RoutingContext.class);
        HttpServerResponse response = mock(HttpServerResponse.class);
        when(response.setStatusCode(Mockito.anyInt())).thenReturn(response);
        when(response.putHeader(Mockito.anyString(), Mockito.anyString())).thenReturn(response);

        when(context.response()).thenReturn(response);

        AsyncResult<?> res = mock(AsyncResult.class);
        when(res.cause()).thenReturn(new InvalidClientRequestException("Invalid request"));

        HttpServerVerticleUtils.handleError(context, res);

        verify(context.response()).setStatusCode(400);
        verify(context.response()).end(new JsonObject().put("message", "Invalid request").encode());
    }

    @Test
    void testHandleErrorOtherException() {
        RoutingContext context = mock(RoutingContext.class);
        HttpServerResponse response = mock(HttpServerResponse.class);
        when(response.setStatusCode(Mockito.anyInt())).thenReturn(response);
        when(response.putHeader(Mockito.anyString(), Mockito.anyString())).thenReturn(response);

        when(context.response()).thenReturn(response);

        AsyncResult<?> res = mock(AsyncResult.class);
        when(res.cause()).thenReturn(new RuntimeException("Server error"));

        HttpServerVerticleUtils.handleError(context, res);

        verify(response).setStatusCode(500);
        verify(response).send("Internal Server Error");
    }
}
