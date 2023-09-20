package dev.maltsev.money.transfer.api;

import dev.maltsev.money.transfer.api.service.impl.QueryService;
import dev.maltsev.money.transfer.api.service.impl.TransferCommandService;
import dev.maltsev.money.transfer.api.service.impl.WithdrawCommandService;
import dev.maltsev.money.transfer.api.verticle.HttpServerVerticle;
import io.vertx.core.Vertx;
import org.sql2o.Sql2o;

import static dev.maltsev.money.transfer.api.dao.DaoUtils.setupDatabase;

public class Application {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Sql2o sql2o = setupDatabase();
        TransferCommandService transferCommandService = new TransferCommandService(sql2o);
        WithdrawCommandService withdrawCommandService = new WithdrawCommandService(sql2o);
        QueryService queryService = new QueryService(sql2o);
        vertx.deployVerticle(new HttpServerVerticle(transferCommandService, withdrawCommandService, queryService));
    }
}
