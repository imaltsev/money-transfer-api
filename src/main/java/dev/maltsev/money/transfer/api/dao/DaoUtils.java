package dev.maltsev.money.transfer.api.dao;

import lombok.NoArgsConstructor;
import org.flywaydb.core.Flyway;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class DaoUtils {
    public static boolean isUniqueConstraintViolation(Sql2oException e) {
        return e.getMessage() != null && e.getMessage().contains("unique constraint or index violation");
    }

    public static Sql2o setupDatabase() {
        String url = "jdbc:hsqldb:mem:mymemdb";
        String user = "SA";
        String password = "";

        Flyway flyway = Flyway.configure().dataSource(url, user, password).load();
        flyway.migrate();
        return new Sql2o(url, user, password);
    }
}
