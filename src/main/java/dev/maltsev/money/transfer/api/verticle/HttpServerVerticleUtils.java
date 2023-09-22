package dev.maltsev.money.transfer.api.verticle;

import dev.maltsev.money.transfer.api.service.exception.InvalidClientRequestException;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class HttpServerVerticleUtils {
    static <T> T deserialize(RoutingContext context, Class<T> objectClass) throws InvalidClientRequestException {
        JsonObject jsonBody = getBodyAsJsonObject(context);
        try {
            return jsonBody.mapTo(objectClass);
        } catch (Exception e) {
            throw new InvalidClientRequestException(e.getMessage());
        }
    }

    private static JsonObject getBodyAsJsonObject(RoutingContext context) throws InvalidClientRequestException {
        RequestBody body = context.body();
        if (body == null || body.isEmpty()) {
            throw new InvalidClientRequestException("Request body is required");
        }

        try {
            return body.asJsonObject();
        } catch (DecodeException e) {
            throw new InvalidClientRequestException("Request body is not valid JSON");
        }
    }

    static void handleError(RoutingContext context, AsyncResult<?> res) {
        if (res.cause() instanceof InvalidClientRequestException) {
            context.response().setStatusCode(400).putHeader("content-type", "application/json")
                    .end(new JsonObject().put("message", res.cause().getMessage()).encode());
        } else {
            context.response().setStatusCode(500).putHeader("content-type", "application/text").send("Internal Server Error");
        }
    }
}
