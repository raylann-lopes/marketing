CREATE TABLE tb_task(
    id BIGSERIAL PRIMARY KEY,
    title varchar(40) NOT NULL,
    description varchar(500),
    client_id BIGINT,
    client_name varchar(120),
    user_id BIGINT NOT NULL,
    date_expires DATE NOT NULL ,
    time_expires TIME,
    type varchar(15) NOT NULL ,
    priority varchar(15) NOT NULL ,
    status varchar(15) NOT NULL ,
    source varchar(15) NOT NULL ,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp,

    CONSTRAINT FK_TB_TASK_ON_CLIENT FOREIGN KEY (client_id) REFERENCES tb_client(id),
    CONSTRAINT FK_TB_TASK_ON_USER FOREIGN KEY (user_id) REFERENCES tb_users(id)
);

CREATE INDEX idx_tasks_user_date_expires ON tb_task(user_id, date_expires);
CREATE INDEX idx_task_client_id ON tb_task(client_id);
CREATE INDEX idx_task_status ON tb_task(status);
