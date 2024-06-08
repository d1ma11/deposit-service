CREATE TABLE IF NOT EXISTS bank_accounts
(
    id_bank_account  SERIAL PRIMARY KEY,
    num_bank_account NUMERIC(20, 0) UNIQUE NOT NULL,
    amount           NUMERIC(100, 2)       NOT NULL
);

CREATE TABLE IF NOT EXISTS request_statuses
(
    id_request_status   SERIAL PRIMARY KEY,
    request_status_name VARCHAR(21) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS deposit_types
(
    id_deposit_type   SERIAL PRIMARY KEY,
    deposit_type_name VARCHAR(28) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS types_percent_payment
(
    id_type_percent_payment     SERIAL PRIMARY KEY,
    type_percent_payment_period VARCHAR(13) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS customers
(
    id_customer     SERIAL PRIMARY KEY,
    phone_number    VARCHAR(11) UNIQUE  NOT NULL,
    password        VARCHAR(100) UNIQUE NOT NULL,
    bank_account_id INT                 NOT NULL,

    CONSTRAINT bank_account_id_fk FOREIGN KEY (bank_account_id) REFERENCES bank_accounts (id_bank_account) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS deposits
(
    id_deposit                 SERIAL PRIMARY KEY,
    deposit_refill             BOOLEAN         NOT NULL,
    deposit_withdraw           BOOLEAN         NOT NULL,
    capitalization             BOOLEAN         NOT NULL,
    deposit_amount             NUMERIC(100, 2) NOT NULL,
    start_date                 DATE            NOT NULL,
    end_date                   DATE            NOT NULL,
    deposit_rate               DECIMAL(4, 2)   NOT NULL,
    percent_payment_date       DATE,

    deposit_account_id         INT             NOT NULL,
    deposit_type_id            INT             NOT NULL,
    type_percent_payment_id    INT,
    percent_payment_account_id INT,
    deposit_refund_account_id  INT             NOT NULL,
    customer_id                INT             NOT NULL,

    CONSTRAINT deposit_account_id_fk FOREIGN KEY (deposit_account_id) REFERENCES bank_accounts (id_bank_account) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT percent_payment_account_id_fk FOREIGN KEY (percent_payment_account_id) REFERENCES bank_accounts (id_bank_account) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT deposit_refund_account_id_fk FOREIGN KEY (deposit_refund_account_id) REFERENCES bank_accounts (id_bank_account) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT deposit_type_id_fk FOREIGN KEY (deposit_type_id) REFERENCES deposit_types (id_deposit_type) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT type_percent_payment_id_fk FOREIGN KEY (type_percent_payment_id) REFERENCES types_percent_payment (id_type_percent_payment) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT customer_id_fk FOREIGN KEY (customer_id) REFERENCES customers (id_customer) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS requests
(
    id_request     SERIAL PRIMARY KEY,
    request_date   DATE            NOT NULL,
    deposit_amount NUMERIC(100, 2) NOT NULL,

    customer_id    INT             NOT NULL,
    deposit_id     INT,

    CONSTRAINT customer_id_fk FOREIGN KEY (customer_id) REFERENCES customers (id_customer) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT deposit_id_fk FOREIGN KEY (deposit_id) REFERENCES deposits (id_deposit) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS current_request_status
(
    request_id        SERIAL      NOT NULL,
    request_status_id SERIAL      NOT NULL,

    change_datetime   TIMESTAMPTZ NOT NULL,

    PRIMARY KEY (request_id, request_status_id),
    CONSTRAINT request_id_fk FOREIGN KEY (request_id) REFERENCES requests (id_request) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT request_status_id_fk FOREIGN KEY (request_status_id) REFERENCES request_statuses (id_request_status) ON DELETE CASCADE ON UPDATE CASCADE
);