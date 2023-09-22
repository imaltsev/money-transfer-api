package dev.maltsev.money.transfer.api;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import dev.maltsev.money.transfer.api.service.impl.QueryService;
import dev.maltsev.money.transfer.api.service.impl.TransferCommandService;
import dev.maltsev.money.transfer.api.service.impl.WithdrawCommandService;
import dev.maltsev.money.transfer.api.verticle.HttpServerVerticle;
import io.vertx.core.Vertx;
import org.sql2o.Sql2o;

import static dev.maltsev.money.transfer.api.dao.DaoUtils.setupDatabase;

public class Application {

    @Parameter(names = "--help", help = true)
    private boolean help;
    @Parameter(names = {"--recoveryInterval", "-r"}, description = "An interval in milliseconds between transaction recovery attempts", arity = 1)
    private long recoveryInterval = 60_000;

    public static void main(String[] args) {
        Application application = new Application();
        JCommander commander = JCommander.newBuilder()
                .addObject(application)
                .build();
        commander.parse(args);

        if (application.help) {
            commander.usage();
        } else {
            application.run();
        }
    }

    private void run() {
        Vertx vertx = Vertx.vertx();
        Sql2o sql2o = setupDatabase();
        TransferCommandService transferCommandService = new TransferCommandService(sql2o);
        WithdrawCommandService withdrawCommandService = new WithdrawCommandService(sql2o);
        QueryService queryService = new QueryService(sql2o);
        vertx.deployVerticle(new HttpServerVerticle(transferCommandService, withdrawCommandService, queryService, recoveryInterval));
    }
}
