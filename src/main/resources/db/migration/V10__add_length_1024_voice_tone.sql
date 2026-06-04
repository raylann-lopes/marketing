ALTER TABLE tb_client
    ALTER COLUMN drive_link DROP NOT NULL;

ALTER TABLE tb_client
    ALTER COLUMN voice_tone TYPE VARCHAR(1024) USING (voice_tone::VARCHAR(1024));

ALTER TABLE tb_client
    ALTER COLUMN voice_tone SET NOT NULL;