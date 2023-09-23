package dev.maltsev.money.transfer.api;

import com.beust.jcommander.JCommander;
import dev.maltsev.money.transfer.api.service.ICommandService;
import dev.maltsev.money.transfer.api.service.IQueryService;
import dev.maltsev.money.transfer.api.service.impl.QueryService;
import dev.maltsev.money.transfer.api.service.impl.TransferCommandService;
import dev.maltsev.money.transfer.api.service.impl.WithdrawCommandService;
import dev.maltsev.money.transfer.api.verticle.HttpServerVerticle;
import io.vertx.core.Vertx;
import org.sql2o.Sql2o;

import static dev.maltsev.money.transfer.api.dao.DaoUtils.setupDatabase;

public class Application {

    public static void main(String[] args) {
        ApplicationArgs applicationArgs = new ApplicationArgs();
        JCommander commander = JCommander.newBuilder().addObject(applicationArgs).build();
        commander.parse(args);

        if (applicationArgs.isHelp()) {
            commander.usage();
        } else {
            run(applicationArgs);
        }
    }

    private static void run(ApplicationArgs args) {
        Vertx vertx = Vertx.vertx();
        Sql2o sql2o = setupDatabase();
        ICommandService transferCommandService = new TransferCommandService(sql2o);
        ICommandService withdrawCommandService = new WithdrawCommandService(sql2o);
        IQueryService queryService = new QueryService(sql2o);
        vertx.deployVerticle(new HttpServerVerticle(transferCommandService, withdrawCommandService, queryService, args));
    }
}
