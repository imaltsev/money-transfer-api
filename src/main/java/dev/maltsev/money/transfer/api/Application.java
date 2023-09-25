package dev.maltsev.money.transfer.api;

import com.beust.jcommander.JCommander;
import dev.maltsev.money.transfer.api.service.ICommandService;
import dev.maltsev.money.transfer.api.service.IQueryService;
import dev.maltsev.money.transfer.api.service.impl.QueryService;
import dev.maltsev.money.transfer.api.service.impl.TransferCommandService;
import dev.maltsev.money.transfer.api.service.impl.WithdrawCommandService;
import dev.maltsev.money.transfer.api.verticle.HttpServerVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Sql2o;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static dev.maltsev.money.transfer.api.dao.DaoUtils.setupDatabase;

public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        Parameters applicationArgs = new Parameters();
        JCommander commander = JCommander.newBuilder().addObject(applicationArgs).build();
        commander.parse(args);

        if (applicationArgs.isHelp()) {
            commander.usage();
        } else {
            run(applicationArgs);
        }
    }

    private static void run(Parameters params) {
        Vertx vertx = Vertx.vertx();
        Sql2o sql2o = setupDatabase();
        ICommandService transferCommandService = new TransferCommandService(sql2o);
        ICommandService withdrawCommandService = new WithdrawCommandService(sql2o);
        IQueryService queryService = new QueryService(sql2o);
        vertx.deployVerticle(new HttpServerVerticle(transferCommandService, withdrawCommandService, queryService, params));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> destroyVertx(vertx, params)));
    }

    public static void destroyVertx(Vertx vertx, Parameters args) {
        if (vertx == null) {
            throw new IllegalStateException("Vertx has not been initialized!");
        }

        LOG.info("Closing vertx");

        // Create a completable future for the purpose of being able to block the thread
        final CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        // Create a close completion handler which completes the completable future above
        final Handler<AsyncResult<Void>> closeCompletionHandler =
                result -> {
                    if (!result.failed()) {
                        completableFuture.complete(null);
                    } else {
                        completableFuture.completeExceptionally(result.cause());
                    }
                };

        // Async close with reference to completion handler
        vertx.close(closeCompletionHandler);

        try {
            // Wait for the vertx to complete and pickup the result (could be an exception)
            completableFuture.get(args.getShutdownHookTimeout(), TimeUnit.MILLISECONDS);

            LOG.info("Vertx was closed successfully");
        } catch (InterruptedException e) {
            throw new IllegalStateException("Thread was interrupted while waiting to vertx instance to close!", e);
        } catch (ExecutionException e) {
            LOG.error("An execution exception was caught while waiting to vertx instance to close!", e);
        } catch (TimeoutException e) {
            LOG.error("An timeout exception was caught while waiting to vertx instance to close!", e);
        }
    }
}
