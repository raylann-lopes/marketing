-- V15 já foi aplicada em ambientes existentes — não editar aquela migração,
-- corrigir aqui via ALTER.

-- created_at nullable permitia ordenação/auditoria inconsistente
UPDATE tb_comments SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;

ALTER TABLE tb_comments
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE tb_comments
    ALTER COLUMN created_at SET NOT NULL;

-- Postgres não indexa FK automaticamente — sem isso, findByPostIdOrderByCreatedAtAsc
-- e a contagem por post degradam conforme a tabela cresce
CREATE INDEX idx_tb_comments_post_id_created_at ON tb_comments (post_id, created_at);
CREATE INDEX idx_tb_comments_user_id ON tb_comments (user_id);
