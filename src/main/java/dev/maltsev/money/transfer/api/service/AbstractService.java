package dev.maltsev.money.transfer.api.service;

import dev.maltsev.money.transfer.api.logging.Loggable;
import lombok.RequiredArgsConstructor;
import org.sql2o.Sql2o;

@RequiredArgsConstructor
public abstract class AbstractService implements Loggable {

    protected final Sql2o sql;

}
