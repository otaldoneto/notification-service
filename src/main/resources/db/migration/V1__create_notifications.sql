CREATE TABLE notifications (
    id         UUID PRIMARY KEY,
    recipient  VARCHAR(320)  NOT NULL,
    subject    VARCHAR(200)  NOT NULL,
    body       TEXT          NOT NULL,
    status     VARCHAR(20)   NOT NULL,
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ   NOT NULL,
    sent_at    TIMESTAMPTZ
);
