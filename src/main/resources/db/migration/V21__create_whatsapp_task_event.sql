CREATE TABLE tb_whatsapp_task_event (
    id BIGSERIAL PRIMARY KEY,
    message_id varchar(150) NOT NULL,
    payload text NOT NULL,
    status varchar(20) NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamp,
    last_error varchar(500),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_whatsapp_task_event_message_id UNIQUE (message_id)
);

CREATE INDEX idx_whatsapp_task_event_retry
    ON tb_whatsapp_task_event(status, next_attempt_at, updated_at);
