package dev.maltsev.money.transfer.api.service.impl;

import dev.maltsev.money.transfer.api.dao.TransactionDao;
import dev.maltsev.money.transfer.api.domain.object.TransactionStatus;
import dev.maltsev.money.transfer.api.domain.object.TransactionType;
import dev.maltsev.money.transfer.api.service.AbstractService;
import dev.maltsev.money.transfer.api.service.exception.UnknownTransactionException;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import java.util.List;

public class QueryService extends AbstractService {

    public QueryService(Sql2o sql) {
        super(sql);
    }

    public TransactionStatus getTransactionStatus(String transactionId, String payer) throws UnknownTransactionException {
        try (Connection connection = sql.open()) {
            TransactionStatus status = TransactionDao.getTransactionStatus(transactionId, payer, connection);
            if (status == null) {
                String message = "Transaction with id = '%s' and payer = '%s' not found".formatted(transactionId, payer);
                logger().info(message);
                throw new UnknownTransactionException(message);
            } else {
                return status == TransactionStatus.AWAITING ? TransactionStatus.PROCESSING : status;
            }
        }
    }

    public List<String> findAllHandlingTransactionIdsByType(TransactionType transactionType) {
        try (Connection connection = sql.open()) {
            return TransactionDao.findAllHandlingTransactionIdsByType(transactionType, connection);
        }
    }
}
