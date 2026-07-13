-- Score de engajamento do sinal viral que originou a ideia (likes + 2*comments).
-- Usado para ordenacao no frontend e como base para o ranking por nicho.
ALTER TABLE tb_content_ideas
    ADD COLUMN engagement_score DOUBLE PRECISION;

-- Justificativa e resumo do sinal vem da IA e estouram facil 255 chars
ALTER TABLE tb_content_ideas
    ALTER COLUMN reason TYPE VARCHAR(1024);

ALTER TABLE tb_content_ideas
    ALTER COLUMN signal_summary TYPE VARCHAR(1024);

ALTER TABLE tb_content_ideas
    ALTER COLUMN source_terms TYPE VARCHAR(1024);
