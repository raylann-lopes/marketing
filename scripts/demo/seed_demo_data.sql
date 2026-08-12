-- Dados ficticios para capturas de tela e demonstracao local.
-- Este arquivo nao e uma migration e nunca deve ser executado em producao.

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM tb_users
        WHERE role = 'ADMIN' AND active = TRUE
    ) THEN
        RAISE EXCEPTION 'Nenhum administrador ativo encontrado para vincular os dados de demonstracao.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM tb_client
        WHERE email IS NULL OR email NOT LIKE '%@northdemo.local'
    ) THEN
        RAISE EXCEPTION 'O banco possui clientes que nao sao de demonstracao. Seed cancelado por seguranca.';
    END IF;
END $$;

-- Permite executar novamente e reconstruir a mesma demonstracao.
DELETE FROM tb_content_ideas
WHERE client_id IN (SELECT id FROM tb_client WHERE email LIKE '%@northdemo.local');

DELETE FROM tb_content_idea_runs
WHERE client_id IN (SELECT id FROM tb_client WHERE email LIKE '%@northdemo.local');

DELETE FROM tb_comments
WHERE post_id IN (
    SELECT p.id
    FROM tb_posts p
    JOIN tb_client c ON c.id = p.client_id
    WHERE c.email LIKE '%@northdemo.local'
);

DELETE FROM tb_post_carousel_arts
WHERE approve_id IN (
    SELECT a.id
    FROM tb_post_approvals a
    JOIN tb_posts p ON p.id = a.post_id
    JOIN tb_client c ON c.id = p.client_id
    WHERE c.email LIKE '%@northdemo.local'
);

DELETE FROM tb_post_carousel_images
WHERE post_id IN (
    SELECT p.id
    FROM tb_posts p
    JOIN tb_client c ON c.id = p.client_id
    WHERE c.email LIKE '%@northdemo.local'
);

DELETE FROM tb_post_approvals
WHERE post_id IN (
    SELECT p.id
    FROM tb_posts p
    JOIN tb_client c ON c.id = p.client_id
    WHERE c.email LIKE '%@northdemo.local'
);

DELETE FROM tb_task
WHERE client_id IN (SELECT id FROM tb_client WHERE email LIKE '%@northdemo.local')
   OR source_reference LIKE 'DEMO-%';

DELETE FROM tb_finance
WHERE client_id IN (SELECT id FROM tb_client WHERE email LIKE '%@northdemo.local');

DELETE FROM tb_account_config
WHERE client_id IN (SELECT id FROM tb_client WHERE email LIKE '%@northdemo.local');

DELETE FROM tb_posts
WHERE client_id IN (SELECT id FROM tb_client WHERE email LIKE '%@northdemo.local');

DELETE FROM tb_client WHERE email LIKE '%@northdemo.local';
DELETE FROM tb_users WHERE email LIKE '%@northdemo.local';

-- Equipe ficticia para a tela de usuarios.
INSERT INTO tb_users (name, email, password, role, created_at, active)
VALUES
    ('Marina Costa', 'marina@northdemo.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', CURRENT_TIMESTAMP - INTERVAL '8 months', TRUE),
    ('Lucas Almeida', 'lucas@northdemo.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', CURRENT_TIMESTAMP - INTERVAL '6 months', TRUE),
    ('Bianca Freitas', 'bianca@northdemo.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', CURRENT_TIMESTAMP - INTERVAL '4 months', TRUE),
    ('Rafael Martins', 'rafael@northdemo.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'USER', CURRENT_TIMESTAMP - INTERVAL '2 months', FALSE);

-- Carteira ficticia da agencia.
INSERT INTO tb_client (
    name, email, number, drive_link, niche, voice_tone, status, created_at,
    whatsapp_group_id, whatsapp_group_name, monthly_value, ai_terms
)
VALUES
    ('Aurora Odontologia', 'contato.aurora@northdemo.local', '5500000000001', 'https://drive.google.com/drive/folders/demo-aurora', 'Saude e odontologia', 'Acolhedor, educativo e seguro. Explica temas tecnicos em linguagem simples.', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '11 months', 'demo-group-aurora', 'Aurora | Conteudo', 4200.00, '{"hashtags":["#saudebucal","#odontologia"],"searchTerms":["saude bucal","implantes","estetica dental"]}'::jsonb),
    ('Horizonte Imoveis', 'contato.horizonte@northdemo.local', '5500000000002', 'https://drive.google.com/drive/folders/demo-horizonte', 'Mercado imobiliario', 'Consultivo, objetivo e sofisticado, com foco em seguranca e investimento.', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '10 months', 'demo-group-horizonte', 'Horizonte | Marketing', 5600.00, '{"hashtags":["#imoveis","#investimento"],"searchTerms":["apartamentos","investimento imobiliario","primeiro imovel"]}'::jsonb),
    ('Atlas Performance', 'contato.atlas@northdemo.local', '5500000000003', 'https://drive.google.com/drive/folders/demo-atlas', 'Academia e bem-estar', 'Energetico, motivador e pratico, sem promessas irreais de resultado.', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '9 months', 'demo-group-atlas', 'Atlas | Conteudo', 3800.00, '{"hashtags":["#treino","#bemestar"],"searchTerms":["treino funcional","qualidade de vida","performance"]}'::jsonb),
    ('Verde Vivo', 'contato.verdevivo@northdemo.local', '5500000000004', 'https://drive.google.com/drive/folders/demo-verde', 'Paisagismo e decoracao', 'Inspirador, visual e proximo, valorizando ambientes naturais e bem-estar.', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '8 months', 'demo-group-verde', 'Verde Vivo | Social', 3100.00, '{"hashtags":["#paisagismo","#decoracao"],"searchTerms":["paisagismo","plantas em casa","decoracao afetiva"]}'::jsonb),
    ('Estudio Lume', 'contato.lume@northdemo.local', '5500000000005', 'https://drive.google.com/drive/folders/demo-lume', 'Arquitetura e interiores', 'Elegante, autoral e acessivel, destacando processo, materiais e funcionalidade.', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '7 months', 'demo-group-lume', 'Lume | Projetos', 4900.00, '{"hashtags":["#arquitetura","#interiores"],"searchTerms":["arquitetura","interiores","reforma"]}'::jsonb),
    ('Casa Nativa', 'contato.casanativa@northdemo.local', '5500000000006', 'https://drive.google.com/drive/folders/demo-nativa', 'Gastronomia', 'Afetivo, sensorial e convidativo, com foco em experiencias e ingredientes locais.', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '6 months', 'demo-group-nativa', 'Casa Nativa | Conteudo', 3500.00, '{"hashtags":["#gastronomia","#comidaafetiva"],"searchTerms":["gastronomia brasileira","comida afetiva","ingredientes locais"]}'::jsonb),
    ('Orbe Tecnologia', 'contato.orbe@northdemo.local', '5500000000007', 'https://drive.google.com/drive/folders/demo-orbe', 'Tecnologia B2B', 'Claro, confiavel e orientado a resultados, evitando excesso de jargoes.', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '5 months', 'demo-group-orbe', 'Orbe | Marketing', 6200.00, '{"hashtags":["#automacao","#tecnologia"],"searchTerms":["automacao","gestao de dados","produtividade"]}'::jsonb),
    ('Pet Vale', 'contato.petvale@northdemo.local', '5500000000008', 'https://drive.google.com/drive/folders/demo-petvale', 'Clinica veterinaria', 'Carinhoso, responsavel e educativo, priorizando o cuidado preventivo.', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '4 months', 'demo-group-petvale', 'Pet Vale | Conteudo', 2900.00, '{"hashtags":["#saudeanimal","#pets"],"searchTerms":["saude animal","cuidados com pets","medicina preventiva"]}'::jsonb),
    ('Movimento Criativo', 'contato.movimento@northdemo.local', '5500000000009', 'https://drive.google.com/drive/folders/demo-movimento', 'Eventos corporativos', 'Criativo, dinamico e profissional, com foco em experiencia de marca.', 'INACTIVE', CURRENT_TIMESTAMP - INTERVAL '14 months', NULL, NULL, 2700.00, '{"hashtags":["#eventos","#experienciademarca"],"searchTerms":["eventos","experiencia de marca","ativacoes"]}'::jsonb);

-- Board e calendario editorial. As datas acompanham o dia em que o seed for executado.
WITH admin_user AS (
    SELECT id FROM tb_users WHERE role = 'ADMIN' AND active = TRUE ORDER BY id LIMIT 1
), post_data(client_email, title, theme, objective, status, day_offset, post_time, urgent, format, created_offset) AS (
    VALUES
        ('contato.aurora@northdemo.local', 'Sinais de que e hora de visitar o dentista', 'Prevencao odontologica', 'Gerar conscientizacao e agendamentos', 'DEMAND', 2, TIME '09:00', FALSE, 'CAROUSEL', 0),
        ('contato.horizonte@northdemo.local', 'Checklist do primeiro apartamento', 'Compra do primeiro imovel', 'Captar leads qualificados', 'DEMAND', 4, TIME '10:30', TRUE, 'CAROUSEL', -1),
        ('contato.verdevivo@northdemo.local', 'Plantas ideais para apartamentos', 'Paisagismo interno', 'Aumentar salvamentos', 'DEMAND', 5, TIME '17:00', FALSE, 'CAROUSEL', -1),
        ('contato.petvale@northdemo.local', 'Cuidados com pets no inverno', 'Saude preventiva', 'Educar tutores', 'DEMAND', 6, TIME '11:00', FALSE, 'IMAGE', -2),

        ('contato.atlas@northdemo.local', 'Treino rapido para uma rotina corrida', 'Treino funcional', 'Gerar compartilhamentos', 'IN_PRODUCTION', 1, TIME '18:30', TRUE, 'CAROUSEL', -3),
        ('contato.lume@northdemo.local', 'Antes e depois de uma sala integrada', 'Projeto residencial', 'Apresentar portfolio', 'IN_PRODUCTION', 3, TIME '19:00', FALSE, 'CAROUSEL', -4),
        ('contato.orbe@northdemo.local', 'Automacao que libera tempo da equipe', 'Produtividade empresarial', 'Gerar oportunidades comerciais', 'IN_PRODUCTION', 5, TIME '08:30', FALSE, 'IMAGE', -2),
        ('contato.casanativa@northdemo.local', 'Bastidores do novo menu sazonal', 'Gastronomia regional', 'Aproximar marca e publico', 'IN_PRODUCTION', 2, TIME '12:00', FALSE, 'IMAGE', -3),

        ('contato.horizonte@northdemo.local', 'Tour pelo empreendimento Vista Alta', 'Lancamento imobiliario', 'Apresentar diferenciais do produto', 'REJECTED', 3, TIME '16:00', TRUE, 'CAROUSEL', -5),
        ('contato.aurora@northdemo.local', 'Mitos e verdades sobre clareamento', 'Estetica dental', 'Reduzir objecoes de pacientes', 'REJECTED', 4, TIME '14:00', FALSE, 'CAROUSEL', -6),

        ('contato.verdevivo@northdemo.local', 'Como montar um jardim vertical', 'Decoracao natural', 'Estimular pedidos de orcamento', 'FINISHED', 1, TIME '15:00', FALSE, 'CAROUSEL', -5),
        ('contato.petvale@northdemo.local', 'Vacinas essenciais para caes e gatos', 'Calendario de vacinacao', 'Gerar agendamentos preventivos', 'FINISHED', 2, TIME '09:30', FALSE, 'CAROUSEL', -6),
        ('contato.orbe@northdemo.local', '5 indicadores para acompanhar na operacao', 'Gestao orientada a dados', 'Construir autoridade', 'FINISHED', 4, TIME '11:30', FALSE, 'CAROUSEL', -4),

        ('contato.casanativa@northdemo.local', 'Sabores que contam historias', 'Ingredientes locais', 'Fortalecer posicionamento', 'WAITING_APPROVAL', 1, TIME '11:30', FALSE, 'IMAGE', -7),
        ('contato.lume@northdemo.local', 'Materiais naturais em projetos contemporaneos', 'Design biofilico', 'Inspirar potenciais clientes', 'WAITING_APPROVAL', 2, TIME '18:00', FALSE, 'CAROUSEL', -5),
        ('contato.atlas@northdemo.local', 'Constancia vale mais que intensidade', 'Habitos saudaveis', 'Aumentar engajamento', 'WAITING_APPROVAL', 3, TIME '07:30', TRUE, 'IMAGE', -4),

        ('contato.aurora@northdemo.local', 'Guia de higiene bucal para toda familia', 'Prevencao em familia', 'Educar e gerar salvamentos', 'SCHEDULE', 1, TIME '19:30', FALSE, 'CAROUSEL', -8),
        ('contato.horizonte@northdemo.local', '3 bairros para investir este ano', 'Investimento imobiliario', 'Atrair investidores', 'SCHEDULE', 2, TIME '12:30', FALSE, 'CAROUSEL', -7),
        ('contato.verdevivo@northdemo.local', 'Um cantinho verde muda a rotina', 'Bem-estar em casa', 'Gerar identificacao', 'SCHEDULE', 4, TIME '20:00', FALSE, 'IMAGE', -6),
        ('contato.orbe@northdemo.local', 'Sua equipe ainda perde tempo com planilhas?', 'Automacao de processos', 'Gerar demonstracoes comerciais', 'SCHEDULE', 6, TIME '10:00', TRUE, 'IMAGE', -5),

        ('contato.atlas@northdemo.local', 'Mobilidade antes do treino', 'Prevencao de lesoes', 'Entregar valor pratico', 'PUBLISHED', -1, TIME '07:00', FALSE, 'CAROUSEL', -12),
        ('contato.casanativa@northdemo.local', 'Prato da semana: sabores da serra', 'Menu executivo', 'Gerar visitas ao restaurante', 'PUBLISHED', -2, TIME '11:00', FALSE, 'IMAGE', -13),
        ('contato.petvale@northdemo.local', 'Quando levar o pet ao veterinario', 'Sinais de alerta', 'Promover cuidado preventivo', 'PUBLISHED', -4, TIME '18:00', FALSE, 'CAROUSEL', -15),
        ('contato.lume@northdemo.local', 'Iluminacao que transforma ambientes', 'Projeto luminotecnico', 'Demonstrar especialidade', 'PUBLISHED', -6, TIME '19:00', FALSE, 'CAROUSEL', -17),
        ('contato.horizonte@northdemo.local', 'Documentos para financiar seu imovel', 'Financiamento imobiliario', 'Reduzir duvidas do comprador', 'PUBLISHED', -8, TIME '12:00', FALSE, 'CAROUSEL', -19),
        ('contato.aurora@northdemo.local', 'O que causa sensibilidade nos dentes?', 'Saude bucal', 'Educar pacientes', 'PUBLISHED', -10, TIME '18:30', FALSE, 'IMAGE', -21)
)
INSERT INTO tb_posts (
    client_id, user_id, title, theme, objective, status, scheduled_at, created_at,
    is_urgent, format
)
SELECT
    c.id,
    a.id,
    p.title,
    p.theme,
    p.objective,
    p.status,
    CURRENT_DATE + p.day_offset + p.post_time,
    CURRENT_TIMESTAMP + (p.created_offset || ' days')::interval,
    p.urgent,
    p.format
FROM post_data p
JOIN tb_client c ON c.email = p.client_email
CROSS JOIN admin_user a;

-- Historico de aprovacoes ficticias. As chaves nao apontam para arquivos reais.
-- A tela continua funcional e utiliza seu estado vazio para as miniaturas.
WITH approval_data(post_title, approval_status, approved_user, rejection_reason, response_text) AS (
    VALUES
        ('Tour pelo empreendimento Vista Alta', 'REJECTED', '', 'Destacar mais as areas de lazer e reduzir o texto da capa.', 'Solicitar ajustes'),
        ('Mitos e verdades sobre clareamento', 'REJECTED', '', 'Usar uma linguagem mais acolhedora na segunda arte.', 'Precisa ajustar'),
        ('Sabores que contam historias', 'PENDING', '', NULL, NULL),
        ('Materiais naturais em projetos contemporaneos', 'PENDING', '', NULL, NULL),
        ('Constancia vale mais que intensidade', 'PENDING', '', NULL, NULL),
        ('Guia de higiene bucal para toda familia', 'APPROVE', 'Equipe Aurora', NULL, 'Aprovado'),
        ('3 bairros para investir este ano', 'APPROVE', 'Equipe Horizonte', NULL, 'Aprovado'),
        ('Um cantinho verde muda a rotina', 'APPROVE', 'Equipe Verde Vivo', NULL, 'Aprovado'),
        ('Sua equipe ainda perde tempo com planilhas?', 'APPROVE', 'Equipe Orbe', NULL, 'Aprovado'),
        ('Mobilidade antes do treino', 'APPROVE', 'Equipe Atlas', NULL, 'Aprovado'),
        ('Prato da semana: sabores da serra', 'APPROVE', 'Equipe Casa Nativa', NULL, 'Aprovado'),
        ('Quando levar o pet ao veterinario', 'APPROVE', 'Equipe Pet Vale', NULL, 'Aprovado'),
        ('Iluminacao que transforma ambientes', 'APPROVE', 'Equipe Lume', NULL, 'Aprovado')
)
INSERT INTO tb_post_approvals (
    post_id, art_s3_key, art_name, caption, status, approved_at, approved_user,
    whatsapp_stanza_id, whatsapp_sent_at, whatsapp_response_text,
    rejection_reason, rejected_at, rejected_by, internal_revision_notes
)
SELECT
    p.id,
    'public/posts/demo/' || p.id || '/arte-demonstrativa.jpg',
    'arte-demonstrativa.jpg',
    CASE p.title
        WHEN 'Sabores que contam historias' THEN 'Cada ingrediente carrega uma origem, uma memoria e um sabor. Na Casa Nativa, cada prato celebra historias da nossa regiao.'
        WHEN 'Materiais naturais em projetos contemporaneos' THEN 'Texturas naturais criam ambientes acolhedores sem abrir mao da funcionalidade e da personalidade.'
        WHEN 'Constancia vale mais que intensidade' THEN 'Resultados sustentaveis nascem de uma rotina possivel. Comece pequeno, mantenha o ritmo e evolua todos os dias.'
        ELSE p.objective || '. Conteudo demonstrativo para apresentacao do fluxo de aprovacao.'
    END,
    d.approval_status,
    CASE WHEN d.approval_status = 'APPROVE' THEN CURRENT_TIMESTAMP - INTERVAL '2 days' ELSE NULL END,
    d.approved_user,
    'DEMO-STANZA-' || p.id,
    (CURRENT_TIMESTAMP - INTERVAL '3 days')::text,
    d.response_text,
    d.rejection_reason,
    CASE WHEN d.approval_status = 'REJECTED' THEN CURRENT_TIMESTAMP - INTERVAL '1 day' ELSE NULL END,
    CASE WHEN d.approval_status = 'REJECTED' THEN 'Cliente demonstrativo' ELSE NULL END,
    CASE WHEN d.approval_status = 'APPROVE' THEN 'Revisao interna concluida antes do envio.' ELSE NULL END
FROM approval_data d
JOIN tb_posts p ON p.title = d.post_title;

-- Comentarios internos para enriquecer os detalhes do board.
WITH admin_user AS (
    SELECT id FROM tb_users WHERE role = 'ADMIN' AND active = TRUE ORDER BY id LIMIT 1
), comment_data(post_title, comment_text, created_offset) AS (
    VALUES
        ('Treino rapido para uma rotina corrida', 'Usar fotos com movimentos simples e acessiveis para iniciantes.', -2),
        ('Treino rapido para uma rotina corrida', 'Roteiro revisado. Falta finalizar a ultima arte do carrossel.', -1),
        ('Antes e depois de uma sala integrada', 'Cliente enviou as imagens finais em alta resolucao.', -2),
        ('Tour pelo empreendimento Vista Alta', 'Ajustar a capa conforme retorno recebido na aprovacao.', -1),
        ('Automacao que libera tempo da equipe', 'Incluir um exemplo pratico antes da chamada comercial.', 0),
        ('Plantas ideais para apartamentos', 'Separar opcoes para ambientes com pouca e muita luz.', 0)
)
INSERT INTO tb_comments (post_id, user_id, text, created_at)
SELECT p.id, a.id, d.comment_text, CURRENT_TIMESTAMP + (d.created_offset || ' days')::interval
FROM comment_data d
JOIN tb_posts p ON p.title = d.post_title
CROSS JOIN admin_user a;

-- Agenda operacional: pendencias de hoje, atrasadas, futuras e concluidas.
WITH admin_user AS (
    SELECT id FROM tb_users WHERE role = 'ADMIN' AND active = TRUE ORDER BY id LIMIT 1
), task_data(client_email, client_name, title, description, day_offset, task_time, task_type, priority, task_status, source, source_reference) AS (
    VALUES
        ('contato.aurora@northdemo.local', 'Aurora Odontologia', 'Confirmar fotos da equipe', 'Cobrar as fotos profissionais combinadas para o carrossel institucional.', 0, TIME '09:00', 'COBRANCA', 'HIGH', 'PENDING', 'WHATSAPP', 'DEMO-TASK-001'),
        ('contato.horizonte@northdemo.local', 'Horizonte Imoveis', 'Revisar roteiro do lancamento', 'Validar diferenciais, metragem e condicoes comerciais antes da producao.', 0, TIME '10:30', 'TAREFA', 'URGENT', 'PENDING', 'MANUAL', 'DEMO-TASK-002'),
        ('contato.casanativa@northdemo.local', 'Casa Nativa', 'Reuniao de pauta mensal', 'Alinhar campanhas, pratos sazonais e datas especiais do proximo mes.', 0, TIME '11:30', 'REUNIAO', 'NORMAL', 'PENDING', 'MANUAL', 'DEMO-TASK-003'),
        ('contato.orbe@northdemo.local', 'Orbe Tecnologia', 'Enviar previa da campanha', 'Compartilhar a primeira versao dos criativos e registrar o retorno.', 0, TIME '14:00', 'PRAZO', 'HIGH', 'PENDING', 'WHATSAPP', 'DEMO-TASK-004'),
        ('contato.verdevivo@northdemo.local', 'Verde Vivo', 'Agendar gravacao no showroom', 'Confirmar disponibilidade da equipe e organizar a lista de cenas.', 0, TIME '15:30', 'LEMBRETE', 'NORMAL', 'PENDING', 'WHATSAPP', 'DEMO-TASK-005'),
        ('contato.atlas@northdemo.local', 'Atlas Performance', 'Acompanhar aprovacao do carrossel', 'Verificar retorno do cliente e liberar a publicacao no calendario.', 0, TIME '17:00', 'FOLLOW_UP', 'URGENT', 'PENDING', 'MANUAL', 'DEMO-TASK-006'),
        ('contato.petvale@northdemo.local', 'Pet Vale', 'Cobrar calendario de vacinacao', 'Solicitar as datas da campanha preventiva para fechar a pauta.', -2, TIME '16:00', 'COBRANCA', 'HIGH', 'PENDING', 'WHATSAPP', 'DEMO-TASK-007'),
        ('contato.lume@northdemo.local', 'Estudio Lume', 'Validar creditos das fotografias', 'Confirmar os profissionais que devem ser marcados na publicacao.', -1, TIME '18:00', 'TAREFA', 'NORMAL', 'PENDING', 'MANUAL', 'DEMO-TASK-008'),
        ('contato.aurora@northdemo.local', 'Aurora Odontologia', 'Preparar pauta de depoimentos', 'Selecionar perguntas curtas para gravar com pacientes autorizados.', 1, TIME '09:30', 'TAREFA', 'NORMAL', 'PENDING', 'MANUAL', 'DEMO-TASK-009'),
        ('contato.horizonte@northdemo.local', 'Horizonte Imoveis', 'Follow-up da campanha de leads', 'Conferir qualidade dos contatos e registrar as principais objecoes.', 2, TIME '10:00', 'FOLLOW_UP', 'HIGH', 'PENDING', 'WHATSAPP', 'DEMO-TASK-010'),
        ('contato.casanativa@northdemo.local', 'Casa Nativa', 'Fotografar menu executivo', 'Captar pratos, detalhes do ambiente e atendimento no horario de almoco.', 3, TIME '12:00', 'LEMBRETE', 'NORMAL', 'PENDING', 'MANUAL', 'DEMO-TASK-011'),
        ('contato.orbe@northdemo.local', 'Orbe Tecnologia', 'Planejar webinar de automacao', 'Definir tema, convidado, pagina de inscricao e sequencia de divulgacao.', 5, TIME '14:30', 'REUNIAO', 'LOW', 'PENDING', 'MANUAL', 'DEMO-TASK-012'),
        ('contato.verdevivo@northdemo.local', 'Verde Vivo', 'Aprovar selecao de ambientes', 'Cliente aprovou as imagens que entram no carrossel da semana.', -1, TIME '11:00', 'TAREFA', 'NORMAL', 'DONE', 'MANUAL', 'DEMO-TASK-013'),
        ('contato.atlas@northdemo.local', 'Atlas Performance', 'Enviar relatorio quinzenal', 'Relatorio enviado com alcance, engajamento e proximas oportunidades.', -3, TIME '17:30', 'PRAZO', 'HIGH', 'DONE', 'MANUAL', 'DEMO-TASK-014'),
        ('contato.petvale@northdemo.local', 'Pet Vale', 'Confirmar participacao da veterinaria', 'A gravacao foi remarcada pelo cliente para o proximo ciclo.', 4, TIME '09:00', 'REUNIAO', 'NORMAL', 'CANCELED', 'MANUAL', 'DEMO-TASK-015')
)
INSERT INTO tb_task (
    title, description, client_id, client_name, user_id, date_expires, time_expires,
    type, priority, status, source, source_reference, created_at, updated_at
)
SELECT
    d.title,
    d.description,
    c.id,
    d.client_name,
    a.id,
    CURRENT_DATE + d.day_offset,
    d.task_time,
    d.task_type,
    d.priority,
    d.task_status,
    d.source,
    d.source_reference,
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    CASE WHEN d.task_status <> 'PENDING' THEN CURRENT_TIMESTAMP - INTERVAL '1 day' ELSE NULL END
FROM task_data d
JOIN tb_client c ON c.email = d.client_email
CROSS JOIN admin_user a;

-- Financeiro do mes atual, com receitas, despesas, pagos e pendentes.
WITH admin_user AS (
    SELECT id FROM tb_users WHERE role = 'ADMIN' AND active = TRUE ORDER BY id LIMIT 1
), finance_data(client_email, description, amount, finance_status, finance_type, day_offset) AS (
    VALUES
        ('contato.aurora@northdemo.local', 'Mensalidade de marketing', 4200.00, 'PAY', 'FIXED_REVENUE', -7),
        ('contato.horizonte@northdemo.local', 'Mensalidade de marketing', 5600.00, 'PAY', 'FIXED_REVENUE', -6),
        ('contato.atlas@northdemo.local', 'Mensalidade de marketing', 3800.00, 'PENDING', 'FIXED_REVENUE', 2),
        ('contato.verdevivo@northdemo.local', 'Mensalidade de marketing', 3100.00, 'PAY', 'FIXED_REVENUE', -5),
        ('contato.lume@northdemo.local', 'Mensalidade de marketing', 4900.00, 'PENDING', 'FIXED_REVENUE', 3),
        ('contato.casanativa@northdemo.local', 'Mensalidade de marketing', 3500.00, 'PAY', 'FIXED_REVENUE', -4),
        ('contato.orbe@northdemo.local', 'Mensalidade de marketing', 6200.00, 'PENDING', 'FIXED_REVENUE', 5),
        ('contato.petvale@northdemo.local', 'Mensalidade de marketing', 2900.00, 'PAY', 'FIXED_REVENUE', -3),
        ('contato.horizonte@northdemo.local', 'Campanha de lancamento', 1800.00, 'PENDING', 'VARIABLE_REVENUE', 8),
        ('contato.casanativa@northdemo.local', 'Producao fotografica especial', 1200.00, 'PAY', 'VARIABLE_REVENUE', -2),
        ('contato.aurora@northdemo.local', 'Impulsionamento de campanha', 850.00, 'PAY', 'VARIABLE_EXPENSE', -2),
        ('contato.lume@northdemo.local', 'Fotografia de interiores', 1400.00, 'PENDING', 'VARIABLE_EXPENSE', 4),
        ('contato.orbe@northdemo.local', 'Ferramentas de automacao', 690.00, 'PAY', 'FIXED_EXPENSE', -1),
        ('contato.atlas@northdemo.local', 'Edicao de video adicional', 780.00, 'PENDING', 'VARIABLE_EXPENSE', 6)
)
INSERT INTO tb_finance (
    client_id, user_id, description, value, status, expiration_date,
    payment_date, created_at, type
)
SELECT
    c.id,
    a.id,
    d.description,
    d.amount,
    d.finance_status,
    CURRENT_DATE + d.day_offset + TIME '12:00',
    CASE
        WHEN d.finance_status = 'PAY' THEN CURRENT_DATE + d.day_offset + TIME '10:00'
        ELSE CURRENT_DATE + d.day_offset + TIME '12:00'
    END,
    CURRENT_TIMESTAMP - INTERVAL '10 days',
    d.finance_type
FROM finance_data d
JOIN tb_client c ON c.email = d.client_email
CROSS JOIN admin_user a;

-- Ideias ficticias geradas por sinais de conteudo.
WITH idea_data(client_email, title, theme, objective, format, hook, reason, source_terms, signal_summary, score, idea_status, priority, created_offset) AS (
    VALUES
        ('contato.aurora@northdemo.local', 'O erro mais comum na escovacao', 'Prevencao odontologica', 'Gerar salvamentos', 'REELS', 'Voce pode estar escovando os dentes do jeito errado', 'Tema recorrente com forte potencial educativo e alta taxa de compartilhamento.', 'escovacao correta, saude bucal', 'Videos curtos com demonstracao pratica tiveram crescimento de interacoes.', 18400.0, 'SUGGESTED', 'HIGH', 0),
        ('contato.horizonte@northdemo.local', 'Quanto custa adiar a compra do imovel?', 'Planejamento financeiro', 'Gerar leads', 'CAROUSEL', 'Esperar o momento perfeito tambem tem um custo', 'Abordagem comparativa ajuda o publico a entender cenarios sem promessa comercial.', 'financiamento, valorizacao imobiliaria', 'Conteudos com simulacoes simples geraram muitos salvamentos.', 12900.0, 'SUGGESTED', 'HIGH', 0),
        ('contato.atlas@northdemo.local', 'Treino para quem passa o dia sentado', 'Mobilidade', 'Aumentar compartilhamentos', 'REELS', 'Tres movimentos para fazer antes de encerrar o expediente', 'Resolve uma dor cotidiana com aplicacao imediata.', 'mobilidade, rotina de escritorio', 'Sequencias curtas de exercicios apresentaram alto volume de compartilhamentos.', 22100.0, 'SUGGESTED', 'HIGH', -1),
        ('contato.verdevivo@northdemo.local', 'Plantas que sobrevivem a rotina corrida', 'Paisagismo pratico', 'Gerar identificacao', 'CAROUSEL', 'Pouco tempo nao precisa significar uma casa sem verde', 'Une desejo visual e objecao comum de falta de tempo.', 'plantas faceis, decoracao verde', 'Listas praticas de especies resistentes mantiveram engajamento acima da media.', 9800.0, 'SUGGESTED', 'MEDIUM', -1),
        ('contato.lume@northdemo.local', 'Antes de reformar, responda estas perguntas', 'Planejamento de obra', 'Construir autoridade', 'CAROUSEL', 'Uma boa reforma comeca antes da primeira demolicao', 'Checklist demonstra metodo e reduz inseguranca do potencial cliente.', 'reforma, planejamento, arquitetura', 'Checklists de planejamento receberam grande volume de salvamentos.', 15700.0, 'SUGGESTED', 'HIGH', -2),
        ('contato.casanativa@northdemo.local', 'A historia por tras de um ingrediente local', 'Cultura gastronomica', 'Fortalecer marca', 'REELS', 'Este ingrediente viajou menos e conta muito mais', 'Conteudo de origem aproxima produtor, restaurante e consumidor.', 'ingredientes locais, cozinha brasileira', 'Bastidores com produtores tiveram comentarios mais longos e positivos.', 11300.0, 'SUGGESTED', 'MEDIUM', -2),
        ('contato.orbe@northdemo.local', 'Processos que sua equipe nao deveria fazer manualmente', 'Automacao', 'Gerar oportunidades', 'CAROUSEL', 'Se acontece toda semana, talvez possa ser automatizado', 'Provoca diagnostico e conduz naturalmente para a solucao oferecida.', 'automacao de processos, produtividade', 'Listas de tarefas repetitivas geraram conversas comerciais qualificadas.', 19600.0, 'SUGGESTED', 'HIGH', -3),
        ('contato.petvale@northdemo.local', 'Sinais silenciosos de dor nos pets', 'Saude animal', 'Educar tutores', 'CAROUSEL', 'Seu pet nem sempre demonstra dor de forma obvia', 'Tema de utilidade publica com forte potencial de salvamento.', 'comportamento animal, prevencao', 'Conteudos preventivos tiveram alto alcance entre novos seguidores.', 14300.0, 'SUGGESTED', 'HIGH', -3),
        ('contato.aurora@northdemo.local', 'Alimentos que mancham os dentes', 'Estetica dental', 'Educar pacientes', 'STORIES', 'O cafe nao e o unico item dessa lista', 'Formato rapido para sequencia interativa de enquete.', 'clareamento, alimentacao', 'Enquetes de mitos e verdades mantiveram boa retencao.', 7200.0, 'SAVED', 'MEDIUM', -4),
        ('contato.lume@northdemo.local', 'Iluminacao para apartamentos pequenos', 'Design de interiores', 'Inspirar seguidores', 'FEED', 'A luz certa faz o ambiente parecer maior', 'Tema visual alinhado ao portfolio do cliente.', 'iluminacao, apartamento pequeno', 'Comparativos visuais apresentaram boa taxa de compartilhamento.', 8900.0, 'SAVED', 'MEDIUM', -4),
        ('contato.horizonte@northdemo.local', 'Guia de visita ao apartamento decorado', 'Jornada de compra', 'Preparar compradores', 'CAROUSEL', 'Nao visite um decorado sem observar estes pontos', 'Checklist ajuda a qualificar o atendimento comercial.', 'apartamento decorado, compra de imovel', 'Guias de visita receberam comentarios com duvidas comerciais.', 10600.0, 'CONVERTED', 'MEDIUM', -5),
        ('contato.atlas@northdemo.local', 'Desafio extremo de sete dias', 'Performance', 'Gerar alcance', 'REELS', 'Mude seu corpo em uma semana', 'Promessa desalinhada ao posicionamento responsavel da marca.', 'desafio fitness', 'Tendencia com alcance alto, mas baixa aderencia a identidade do cliente.', 25000.0, 'DISMISSED', 'LOW', -6)
)
INSERT INTO tb_content_ideas (
    client_id, title, theme, objective, format, hook, reason, source_terms,
    signal_summary, engagement_score, status, priority, created_at, expires_at
)
SELECT
    c.id,
    d.title,
    d.theme,
    d.objective,
    d.format,
    d.hook,
    d.reason,
    d.source_terms,
    d.signal_summary,
    d.score,
    d.idea_status,
    d.priority,
    CURRENT_TIMESTAMP + (d.created_offset || ' days')::interval,
    CURRENT_TIMESTAMP + INTERVAL '30 days'
FROM idea_data d
JOIN tb_client c ON c.email = d.client_email;

INSERT INTO tb_content_idea_runs (
    client_id, niche, provider, status, started_at, finished_at, ideas_count, error_message
)
SELECT
    c.id,
    c.niche,
    'DEMO',
    'SUCESS',
    CURRENT_TIMESTAMP - INTERVAL '15 minutes',
    CURRENT_TIMESTAMP - INTERVAL '14 minutes',
    8,
    NULL
FROM tb_client c
WHERE c.email = 'contato.aurora@northdemo.local';

COMMIT;

SELECT 'usuarios' AS grupo, count(*) AS total FROM tb_users
UNION ALL SELECT 'clientes', count(*) FROM tb_client
UNION ALL SELECT 'posts', count(*) FROM tb_posts
UNION ALL SELECT 'aprovacoes', count(*) FROM tb_post_approvals
UNION ALL SELECT 'tarefas', count(*) FROM tb_task
UNION ALL SELECT 'financeiro', count(*) FROM tb_finance
UNION ALL SELECT 'ideias', count(*) FROM tb_content_ideas
ORDER BY grupo;
