ALTER TABLE tb_task ALTER COLUMN title TYPE varchar(60);

ALTER TABLE tb_task ADD COLUMN source_reference varchar(150);

CREATE UNIQUE INDEX uk_task_source_reference
    ON tb_task(source_reference)
    WHERE source_reference IS NOT NULL;
