-- 1. Formato do post
ALTER TABLE tb_posts ADD COLUMN format VARCHAR(20) NOT NULL DEFAULT 'IMAGE';

-- 2. Artes da aprovação (carrossel)
CREATE TABLE tb_post_carousel_arts (
    id          BIGSERIAL PRIMARY KEY,
    approve_id  BIGINT       NOT NULL REFERENCES tb_post_approvals(id) ON DELETE CASCADE,
    s3_key      VARCHAR(500) NOT NULL,
    art_name    VARCHAR(255),
    sort_order  INT          NOT NULL DEFAULT 0
);
CREATE INDEX idx_carousel_arts_approve ON tb_post_carousel_arts (approve_id, sort_order);

-- 3. Imagens de referência (carrossel)
CREATE TABLE tb_post_carousel_images (
    id          BIGSERIAL PRIMARY KEY,
    post_id     BIGINT        NOT NULL REFERENCES tb_posts(id) ON DELETE CASCADE,
    s3_key      VARCHAR(1000) NOT NULL,
    sort_order  INT           NOT NULL DEFAULT 0
);
CREATE INDEX idx_carousel_images_post ON tb_post_carousel_images (post_id, sort_order);

-- 4. Backfill: chave única atual vira o item de índice 0
INSERT INTO tb_post_carousel_arts (approve_id, s3_key, art_name, sort_order)
SELECT id, art_s3_key, art_name, 0 FROM tb_post_approvals WHERE art_s3_key IS NOT NULL AND art_s3_key <> '';

INSERT INTO tb_post_carousel_images (post_id, s3_key, sort_order)
SELECT id, reference_image_s3_key, 0 FROM tb_posts
WHERE reference_image_s3_key IS NOT NULL AND reference_image_s3_key <> '';
