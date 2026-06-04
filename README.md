# North Produções — Sistema de Gestão de Produção de Conteúdo

Plataforma full-stack para gestão de produção audiovisual e conteúdo digital. Centraliza o fluxo de criação, aprovação via WhatsApp, publicação no Instagram e controle financeiro de uma produtora de conteúdo.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Stack Tecnológica](#stack-tecnológica)
- [Arquitetura](#arquitetura)
- [Funcionalidades](#funcionalidades)
- [Pré-requisitos](#pré-requisitos)
- [Configuração do Ambiente](#configuração-do-ambiente)
- [Rodando o Projeto](#rodando-o-projeto)
- [API Reference](#api-reference)
- [Banco de Dados](#banco-de-dados)
- [Integrações Externas](#integrações-externas)
- [Segurança](#segurança)
- [Deploy](#deploy)

---

## Visão Geral

O sistema gerencia o ciclo completo de produção de conteúdo para clientes de uma agência:

```
Criação do Post → Upload da Arte (S3) → Geração de Legenda (IA) →
Envio para Aprovação (WhatsApp) → Votação do Cliente →
Agendamento → Publicação Automática (Instagram)
```

**Papéis de usuário:**
- **ADMIN** — acesso completo: clientes, posts, aprovações, financeiro, integrações e gestão de usuários
- **USER** — cria e edita posts, visualiza aprovações e dashboard

---

## Stack Tecnológica

### Backend
| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 4.0.6 | Framework web/REST |
| Spring Security | — | Autenticação e autorização |
| Spring Data JPA | — | ORM / acesso a dados |
| Flyway | — | Migrações de banco |
| PostgreSQL | 17 | Banco de dados principal |
| JJWT | 0.13.0 | Geração e validação de JWT |
| Spring AI (OpenAI) | 2.0.0-M4 | Geração de legendas com GPT-4o |
| AWS SDK v2 | 2.44.0 | Upload de arquivos no S3 |
| Lombok | — | Redução de boilerplate |
| Maven | 3.9.9 | Build e dependências |

### Frontend
| Tecnologia | Versão | Uso |
|---|---|---|
| Vue.js | 3.5 | Framework SPA |
| TypeScript | 6.0 | Tipagem estática |
| Vite | 8.0 | Build tool e dev server |
| Vue Router | 5.0 | Roteamento |
| Pinia | 3.0 | Gerenciamento de estado |
| Tailwind CSS | 3.4 | Estilização |
| Lucide Vue | 1.0 | Ícones |
| Zod | 4.0 | Validação de schemas |

---

## Arquitetura

```
backend/
├── src/main/java/com/north/producoes/
│   ├── controller/          # Camada REST (11 controllers)
│   ├── service/             # Regras de negócio (17 services)
│   ├── entity/              # Entidades JPA (6 tabelas principais)
│   ├── repository/          # Repositórios Spring Data (7)
│   ├── dto/                 # DTOs de request/response
│   ├── security/            # JWT, filtros, configuração Spring Security
│   ├── integration/         # Clientes HTTP externos (Meta, Evolution, Apify)
│   └── config/              # Beans de configuração (S3, bootstrap, agendadores)
├── src/main/resources/
│   ├── db/migration/        # Migrações Flyway (V1–V10)
│   └── application.properties
└── frontend/
    ├── src/
    │   ├── views/           # 12 páginas Vue
    │   ├── components/      # 33+ componentes reutilizáveis
    │   ├── services/        # Camada de API (TypeScript)
    │   ├── lib/             # Utilitários e configurações
    │   └── assets/          # Logo e imagens estáticas
    └── package.json
```

---

## Funcionalidades

### Gestão de Conteúdo
- Board Kanban com fluxo de status: `DEMAND → IN_PRODUCTION → WAITING_APPROVAL → SCHEDULE → PUBLISHED`
- Upload de arte e imagem de referência direto para o S3 via URL pré-assinada
- Geração de legenda automática via GPT-4o com base no tema, objetivo e identidade do cliente

### Fluxo de Aprovação via WhatsApp
1. Arte enviada ao grupo do cliente no WhatsApp com legenda
2. Poll de votação criado automaticamente (✅ Aprovar / ❌ Rejeitar)
3. Webhook da Evolution API recebe o voto do cliente
4. Status do post atualizado automaticamente
5. Notificação de rejeição com motivo enviada ao grupo quando necessário

### Publicação Automática
- Agendador verifica posts com status `SCHEDULE` a cada 20 minutos
- Publicação automática no Instagram via Meta Graph API

### Gestão de Clientes
- Cadastro com vínculo a grupo WhatsApp e conta Instagram
- Configuração de tom de voz e nicho para personalização da IA
- Status ACTIVE/INACTIVE com soft delete

### Controle Financeiro (Admin)
- Registro de receitas e despesas por cliente
- Previsão anual agrupada por mês
- Filtros por status (PENDING/PAID) e tipo (INCOME/EXPENSE)

### Gestão de Usuários (Admin)
- Criação, edição, ativação e desativação de usuários
- Troca de papel (ADMIN/USER) sem recriar o usuário

---

## Pré-requisitos

- **Java 21+**
- **Maven 3.9+**
- **Node.js 22+**
- **Docker & Docker Compose** (opcional, recomendado)
- **PostgreSQL 17** (ou via Docker)

---

## Configuração do Ambiente

Crie um arquivo `.env` na raiz do projeto com as seguintes variáveis:

```env
# Banco de Dados
DB_URL=jdbc:postgresql://localhost:5432/producoes
DB_USER=postgres
DB_PASSWORD=sua_senha

# JWT
JWT_KEY=base64_encoded_secret_256bits

# AWS S3
AWS_ACCESS_KEY_ID=sua_access_key
AWS_SECRET_ACCESS_KEY=sua_secret_key
AWS_S3_BUCKET=north-producoes
AWS_S3_REGION=sa-east-1
AWS_S3_PUBLIC_BASE_URL=https://seu-bucket.s3.amazonaws.com

# OpenAI
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4o

# Meta Graph API (Instagram)
META_GRAPH_ACCESS_TOKEN=seu_token
META_GRAPH_API_VERSION=v19.0

# Evolution API (WhatsApp)
EVOLUTION_API_BASE_URL=https://sua-instancia.evolution.com
EVOLUTION_API_KEY=sua_chave
EVOLUTION_API_INSTANCE=nome_da_instancia
EVOLUTION_WEBHOOK_SECRET=uuid_aleatorio

# Apify (Instagram scraping)
APIFY_API_TOKEN=seu_token

# Admin inicial (criado no startup)
BOOTSTRAP_ADMIN_NAME=Admin
BOOTSTRAP_ADMIN_EMAIL=admin@agencianorth.com
BOOTSTRAP_ADMIN_PASSWORD=senha_segura

# Integração interna (n8n)
INTERNAL_API_KEY=chave_para_n8n

# Ambiente
SPRING_PROFILES_ACTIVE=dev
```

**Frontend** — crie `frontend/.env`:
```env
VITE_API_BASE_URL=http://localhost:8080
```

---

## Rodando o Projeto

### Com Docker Compose (recomendado)

```bash
# Sobe PostgreSQL, backend e frontend
docker compose -f docker-compose.local.yml up --build
```

Acesse em `http://localhost`.

### Manualmente

**Backend:**
```bash
# Na raiz do projeto
mvn spring-boot:run
# Disponível em http://localhost:8080
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
# Disponível em http://localhost:5173
```

---

## API Reference

### Autenticação

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| `POST` | `/api/auth/login` | Login com e-mail e senha | Público |
| `POST` | `/api/auth/register` | Criar usuário (admin) | ADMIN |
| `POST` | `/api/auth/refresh` | Renovar access token | Público |

Todas as rotas autenticadas exigem header:
```
Authorization: Bearer <access_token>
```

### Usuários

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| `GET` | `/api/users` | Listar usuários (paginado) | ADMIN |
| `POST` | `/api/users` | Criar usuário | ADMIN |
| `PUT` | `/api/users/id/{id}` | Atualizar usuário | ADMIN |
| `PATCH` | `/api/users/id/{id}/role` | Alterar papel | ADMIN |
| `PATCH` | `/api/users/id/{id}/activate` | Reativar usuário | ADMIN |
| `DELETE` | `/api/users/id/{id}` | Desativar usuário | ADMIN |
| `GET` | `/api/users/me` | Perfil do usuário logado | AUTH |
| `PUT` | `/api/users/me` | Atualizar perfil | AUTH |
| `PUT` | `/api/users/me/password` | Alterar senha | AUTH |

### Clientes

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| `GET` | `/api/clients` | Listar clientes | AUTH |
| `GET` | `/api/clients/status/{status}` | Filtrar por status | AUTH |
| `POST` | `/api/clients` | Criar cliente | ADMIN |
| `PUT` | `/api/clients/{id}` | Atualizar cliente | ADMIN |
| `PATCH` | `/api/clients/{id}/status` | Alterar status | ADMIN |
| `DELETE` | `/api/clients/{id}` | Excluir cliente | ADMIN |

### Posts

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| `GET` | `/api/posts` | Listar posts | AUTH |
| `GET` | `/api/posts/status/{status}` | Filtrar por status | AUTH |
| `GET` | `/api/posts/client/{id}` | Posts de um cliente | AUTH |
| `POST` | `/api/posts/save` | Criar post | ADMIN |
| `PUT` | `/api/posts/update/{id}` | Atualizar post | ADMIN |
| `DELETE` | `/api/posts/delete/{id}` | Excluir post | ADMIN |
| `POST` | `/api/posts/{id}/generate-caption` | Gerar legenda com IA | AUTH |

### Aprovações

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| `GET` | `/api/post-approvals/all` | Listar aprovações | ADMIN |
| `GET` | `/api/post-approvals/{id}` | Buscar por post | AUTH |
| `GET` | `/api/post-approvals/status/{status}` | Filtrar por status | ADMIN |
| `POST` | `/api/post-approvals/save` | Criar aprovação | ADMIN |
| `PUT` | `/api/post-approvals/update/{id}` | Atualizar aprovação | ADMIN |
| `DELETE` | `/api/post-approvals/delete/{id}` | Excluir aprovação | ADMIN |

### Mídia (Upload S3)

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| `GET` | `/api/media/upload-url` | URL pré-assinada para arte | AUTH |
| `GET` | `/api/media/reference-url` | URL pré-assinada para referência | AUTH |
| `POST` | `/api/media/upload-complete` | Confirmar upload concluído | AUTH |
| `GET` | `/api/media/preview/{postId}` | URL de preview da arte | AUTH |

### Financeiro

| Método | Rota | Descrição | Auth |
|---|---|---|---|
| `GET` | `/api/finance` | Listar registros | ADMIN |
| `GET` | `/api/finance/forecast?year=` | Previsão anual | ADMIN |
| `POST` | `/api/finance` | Criar registro | ADMIN |
| `PUT` | `/api/finance/{id}` | Atualizar registro | ADMIN |
| `DELETE` | `/api/finance/{id}` | Excluir registro | ADMIN |

### Integração Interna (n8n)

Requer header `X-Internal-Api-Key: <INTERNAL_API_KEY>`:

| Método | Rota | Descrição |
|---|---|---|
| `PATCH` | `/api/internal/approvals/{id}/status` | Atualizar status de aprovação |
| `PATCH` | `/api/internal/approvals/post/{postId}/approve` | Aprovar post |
| `PATCH` | `/api/internal/approvals/post/{postId}/reject` | Rejeitar post |
| `GET` | `/api/internal/approvals/whatsapp-stanza/{id}` | Buscar por stanza ID |

### Webhooks

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/webhooks/whatsapp/{webhookSecret}` | Receber eventos da Evolution API |

---

## Banco de Dados

### Tabelas Principais

| Tabela | Descrição |
|---|---|
| `tb_users` | Usuários da equipe (ADMIN/USER) |
| `tb_client` | Clientes com dados de integração |
| `tb_posts` | Posts e conteúdos agendados |
| `tb_post_approvals` | Registros de aprovação com metadados WhatsApp |
| `tb_finance` | Movimentações financeiras |
| `tb_account_config` | Configuração de contas Instagram por cliente |
| `tb_refresh_tokens` | Rotação de tokens JWT |

### Migrações Flyway

```
V1  — Schema inicial completo
V2  — Campos de rejeição e correção de status de post
V3  — Notas internas de revisão
V4  — Urgência e imagem de referência
V5  — Renomear status POSTED → PUBLISHED
V6  — Valor mensal por cliente
V7  — Tipo financeiro (INCOME/EXPENSE)
V8  — Colunas monetárias para NUMERIC
V9  — Campo active em usuários (soft delete)
V10 — Length 1024 para voiceTone
```

---

## Integrações Externas

### OpenAI (GPT-4o)
- Geração de legendas personalizadas por cliente
- Usa `spring-ai` com o starter OpenAI
- Endpoint: `POST /api/posts/{id}/generate-caption`

### Meta Graph API (Instagram)
- Publicação automática de posts no Instagram
- Vinculação de conta Instagram por cliente via OAuth
- Versão da API: `v19.0`

### Evolution API (WhatsApp)
- Envio de arte + legenda para grupos de clientes
- Envio de poll de aprovação (✅/❌)
- Recepção de votos via webhook
- Armazenamento do `stanzaId` para correlação de eventos

### AWS S3
- Upload de artes e imagens de referência via URL pré-assinada
- URLs de preview com expiração configurável
- Região padrão: `sa-east-1`

### Apify
- Scraping de dados do Instagram para enriquecimento de conteúdo

---

## Segurança

### Autenticação JWT
- Algoritmo: **HMAC-SHA-256 (HS256)**
- Expiração do access token: **24 horas**
- Rotação de refresh token armazenada com hash no banco

### Filtros de Segurança
- **JwtFilter** — valida Bearer token e popula o `SecurityContext`
- **InternalApiKeyFilter** — protege `/api/internal/**` com `X-Internal-Api-Key` (para automações n8n)

### CORS

| Rota | Origens Permitidas |
|---|---|
| `/**` | `localhost:5173`, `agencianorth.com` e `www.agencianorth.com` |
| `/api/internal/**` | Todas (server-to-server) |
| `/api/webhooks/**` | Todas (Evolution API) |

### Autorização
- `@PreAuthorize` nas rotas administrativas
- Endpoints financeiros e de usuários restritos ao papel **ADMIN**

---

## Deploy

### CI/CD (GitHub Actions)

O pipeline em `.github/workflows/main.yml` detecta mudanças automaticamente:

- **Backend alterado** → build e push da imagem `producoes-api:latest` para o GHCR
- **Frontend alterado** → build e push da imagem `producoes-web:latest` para o GHCR

### Imagens Docker

**Backend** — build multi-stage (Maven → Alpine JRE 21):
```bash
docker build -t producoes-api .
```

**Frontend** — build multi-stage (Node 22 → Nginx Alpine):
```bash
docker build --build-arg VITE_API_BASE_URL=https://api.agencianorth.com -t producoes-web ./frontend
```

### Variáveis de Build (Frontend)
```
VITE_API_BASE_URL   URL pública da API (obrigatório em produção)
```
