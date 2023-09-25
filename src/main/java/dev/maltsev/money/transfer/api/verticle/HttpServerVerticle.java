package dev.maltsev.money.transfer.api.verticle;

import dev.maltsev.money.transfer.api.Parameters;
import dev.maltsev.money.transfer.api.domain.entity.Transaction;
import dev.maltsev.money.transfer.api.domain.json.TransferRequest;
import dev.maltsev.money.transfer.api.domain.json.WithdrawRequest;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.domain.object.TransactionType;
import dev.maltsev.money.transfer.api.logging.Loggable;
import dev.maltsev.money.transfer.api.service.ICommandService;
import dev.maltsev.money.transfer.api.service.IQueryService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Paths;

import static dev.maltsev.money.transfer.api.verticle.HttpServerVerticleUtils.deserialize;
import static dev.maltsev.money.transfer.api.verticle.HttpServerVerticleUtils.handleError;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@RequiredArgsConstructor
public class HttpServerVerticle extends AbstractVerticle implements Loggable {

    private final ICommandService transferCommandService;

    private final ICommandService withdrawCommandService;

    private final IQueryService queryService;

    private final Parameters parameters;

    @SneakyThrows
    private static void handleSwagger(RoutingContext routingContext) {
        routingContext.response()
                .putHeader(CONTENT_TYPE, "application/x-yaml")
                .end(Files.readString(Paths.get("src/main/resources/swagger.yaml")));
    }

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);

        router.route().handler(StaticHandler.create());
        router.route().handler(BodyHandler.create());
        router.post("/customers/:customer/transfer").handler(this::handleTransferRequest);
        router.post("/customers/:customer/withdraw").handler(this::handleWithdrawRequest);
        router.get("/customers/:customer/transactions/:transactionId/status").handler(this::handleGetTransactionStatusRequest);
        router.get("/swagger.yaml").handler(HttpServerVerticle::handleSwagger);

        createHttpServer(startPromise, router);

        // Start background tasks for processing transactions
        runTransfersInBackground();
        runWithdrawalsInBackground();
        runPeriodicRecoverStuckTransactionWatcherInBackground();
    }

    // Periodically check for stuck transactions and attempt to recover them
    private void runPeriodicRecoverStuckTransactionWatcherInBackground() {
        vertx.setPeriodic(parameters.getRecoveryInterval(), id -> {
            vertx.executeBlocking(() -> {
                recoverStuckTransactions();
                return null;
            }, false).onComplete(res -> {
                if (res.failed()) {
                    logger().error("Failed to recover stuck transactions: {}", res.cause().getMessage());
                }
            });
        });
    }

    private void createHttpServer(Promise<Void> startPromise, Router router) {
        vertx.createHttpServer().requestHandler(router).listen(parameters.getPort(), http -> {
            if (http.succeeded()) {
                startPromise.complete();
                logger().info("HTTP server started on port {}", parameters.getPort());
            } else {
                startPromise.fail(http.cause());
            }
        });
    }

    // Process withdrawal requests in background worker threads
    private void runWithdrawalsInBackground() {
        WorkerExecutor withdrawalExecutor = vertx.createSharedWorkerExecutor("withdrawal-worker-pool", parameters.getWithdrawalWorkerPoolSize());
        vertx.eventBus().consumer(TransactionType.WITHDRAWAL.name(), message -> {
            withdrawalExecutor.executeBlocking(() -> {
                String transactionId = (String) message.body();
                TransactionStatus status = withdrawCommandService.executeTransaction(transactionId);
                if (status == TransactionStatus.AWAITING) {
                    vertx.setTimer(1000, id -> vertx.eventBus().publish(TransactionType.WITHDRAWAL.name(), transactionId));
                }
                return null;
            }, false).onComplete(res -> {
                if (res.failed()) {
                    logger().error("Failed to process withdrawal request: {}", res.cause().getMessage());
                }
            });
        });
    }

    // Process transfer requests in background worker threads
    private void runTransfersInBackground() {
        WorkerExecutor transferExecutor = vertx.createSharedWorkerExecutor("transfer-worker-pool", parameters.getTransferWorkerPoolSize());
        vertx.eventBus().consumer(TransactionType.TRANSFER.name(), message -> {
            transferExecutor.executeBlocking(() -> {
                String transactionId = (String) message.body();
                transferCommandService.executeTransaction(transactionId);
                return null;
            }, false).onComplete(res -> {
                if (res.failed()) {
                    logger().error("Failed to process transfer request: {}", res.cause().getMessage());
                }
            });
        });
    }

    // Create and save transaction for transfer requests in background worker thread
    private void handleTransferRequest(RoutingContext context) {
        String customerLogin = context.request().getParam("customer");
        vertx.executeBlocking(() -> {
            TransferRequest request = deserialize(context, TransferRequest.class);
            Transaction transaction = request.toTransaction(customerLogin);
            return transferCommandService.createTransaction(transaction);
        }, false).onComplete(res -> {
            if (res.succeeded()) {
                String transactionId = res.result();
                context.response().putHeader(CONTENT_TYPE, "application/json").end(new JsonObject().put("transactionId", transactionId).encode());
                vertx.eventBus().publish(TransactionType.TRANSFER.name(), transactionId);
            } else {
                handleError(context, res);
            }
        });
    }

    // Create and save transaction for withdrawal requests in background worker thread
    private void handleWithdrawRequest(RoutingContext context) {
        String customerLogin = context.request().getParam("customer");
        vertx.executeBlocking(() -> {
            WithdrawRequest request = deserialize(context, WithdrawRequest.class);
            Transaction transaction = request.toTransaction(customerLogin);
            return withdrawCommandService.createTransaction(transaction);
        }, false).onComplete(res -> {
            if (res.succeeded()) {
                String transactionId = res.result();
                context.response().putHeader(CONTENT_TYPE, "application/json").end(new JsonObject().put("transactionId", transactionId).encode());
                vertx.eventBus().publish(TransactionType.WITHDRAWAL.name(), transactionId);
            } else {
                handleError(context, res);
            }
        });
    }

    private void handleGetTransactionStatusRequest(RoutingContext context) {
        String transactionId = context.request().getParam("transactionId");
        String customerLogin = context.request().getParam("customer");
        vertx.executeBlocking(() -> queryService.getTransactionStatus(transactionId, customerLogin), false).onComplete(res -> {
            if (res.succeeded()) {
                context.response().putHeader(CONTENT_TYPE, "application/json")
                        .end(new JsonObject().put("transactionId", transactionId).put("status", res.result().name()).encode());
            } else {
                handleError(context, res);
            }
        });
    }

    // Recover stuck transactions by re-publishing them to the event bus
    public void recoverStuckTransactions() {
        queryService.getAllStuckTransactions()
                .forEach(transaction -> vertx.eventBus().publish(transaction.type().name(), transaction.id()));
    }
}
