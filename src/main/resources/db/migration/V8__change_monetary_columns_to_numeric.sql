ALTER TABLE tb_finance  ALTER COLUMN value          TYPE NUMERIC(15,2) USING value::NUMERIC(15,2);
ALTER TABLE tb_client   ALTER COLUMN monthly_value  TYPE NUMERIC(15,2) USING monthly_value::NUMERIC(15,2);
