CREATE TABLE customers
(
    login VARCHAR(32) PRIMARY KEY
);

CREATE TABLE accounts
(
    number  VARCHAR(34) PRIMARY KEY,
    balance DECIMAL NOT NULL,
    currency VARCHAR(3) NOT NULL
);

CREATE TABLE customer_accounts
(
    customer_login VARCHAR(32) NOT NULL,
    account_number VARCHAR(34) NOT NULL,
    PRIMARY KEY (customer_login, account_number),
    FOREIGN KEY (customer_login) REFERENCES customers (login),
    FOREIGN KEY (account_number) REFERENCES accounts (number)
);

CREATE TABLE transaction_types
(
    name VARCHAR(32) NOT NULL PRIMARY KEY,
    description VARCHAR(256) NOT NULL
);

INSERT INTO transaction_types (name, description) VALUES ('WITHDRAWAL', 'Withdrawal transaction');
INSERT INTO transaction_types (name, description) VALUES ('TRANSFER', 'Transfer transaction');

CREATE TABLE transaction_statuses
(
    name VARCHAR(32) NOT NULL PRIMARY KEY,
    description VARCHAR(256) NOT NULL
);

INSERT INTO transaction_statuses (name, description) VALUES ('PROCESSING', 'Transaction is currently being processed');
INSERT INTO transaction_statuses (name, description) VALUES ('AWAITING', 'Transaction is currently processed on downstream system side');
INSERT INTO transaction_statuses (name, description) VALUES ('COMPLETED', 'Transaction has been successfully processed');
INSERT INTO transaction_statuses (name, description) VALUES ('FAILED', 'Transaction could not be processed due to an error');

CREATE TABLE transactions
(
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    payer VARCHAR(32) NOT NULL,
    payer_account_number VARCHAR(34) NOT NULL,
    recipient VARCHAR(32),
    recipient_account_number VARCHAR(34),
    withdrawal_address VARCHAR(256),
    amount DECIMAL NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message CLOB,
    created TIMESTAMP NOT NULL,
    updated TIMESTAMP NOT NULL,
    UNIQUE (request_id, payer),
    FOREIGN KEY (payer) REFERENCES customers (login),
    FOREIGN KEY (payer_account_number) REFERENCES accounts (number),
    FOREIGN KEY (recipient) REFERENCES customers (login),
    FOREIGN KEY (recipient_account_number) REFERENCES accounts (number),
    FOREIGN KEY (type) REFERENCES transaction_types (name),
    FOREIGN KEY (status) REFERENCES transaction_statuses (name)
);

CREATE INDEX idx_transactions_status ON transactions(status);

CREATE TABLE transaction_history
(
    id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    UNIQUE (id, status),
    FOREIGN KEY (id) REFERENCES transactions (id),
    FOREIGN KEY (status) REFERENCES transaction_statuses (name)
);

CREATE INDEX idx_transaction_history_id ON transaction_history(id);