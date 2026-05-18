-- Adiciona campos de rejeição à tabela de aprovações
ALTER TABLE tb_post_approvals
    ADD COLUMN rejection_reason VARCHAR(1000),
    ADD COLUMN rejected_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN rejected_by VARCHAR(255);

-- Atualiza a constraint de status da aprovação para incluir REJECTED em vez de REJECT
ALTER TABLE tb_post_approvals DROP CONSTRAINT ck_tb_post_approvals_status_enum;
ALTER TABLE tb_post_approvals ADD CONSTRAINT ck_tb_post_approvals_status_enum CHECK (
    status IN ('APPROVE', 'REJECTED', 'PENDING')
);

-- Atualiza a constraint de status do post para incluir REJECTED e alinhar com o código Java (POSTED em vez de PUBLISHED)
ALTER TABLE tb_posts DROP CONSTRAINT ck_tb_posts_status_enum;
ALTER TABLE tb_posts ADD CONSTRAINT ck_tb_posts_status_enum CHECK (
    status IN ('DEMAND', 'IN_PRODUCTION', 'REJECTED', 'FINISHED', 'WAITING_APPROVAL', 'SCHEDULE', 'POSTED', 'PUBLISHED')
);
