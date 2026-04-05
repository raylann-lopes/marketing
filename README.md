# North Produções

Sistema web para agências de marketing gerenciarem clientes, produção de conteúdo, aprovação de artes e publicação no Instagram com automações via n8n.

---

## Tecnologias

**Backend**

| Stack | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.4 (WebMVC) |
| Spring Security + JWT (jjwt 0.12.6) | — |
| Spring Data JPA + Flyway | — |
| AWS S3 SDK (presigned URLs) | 2.25.0 |
| PostgreSQL | latest |
| SpringDoc OpenAPI (Swagger) | 3.0.2 |
| Lombok | — |

**Frontend** (`frontend_north/`)

| Stack | Versão |
|---|---|
| Vue 3 + TypeScript | 3.5 |
| Vite | 6 |
| Tailwind CSS + shadcn-vue | — |
| Vue Router | 5 |
| Pinia | 3 |
| Zod (validação) | 4 |
| oxlint + eslint + prettier | — |
| Vitest + Cypress | — |

**Automações**

- **n8n** — orquestra geração de legendas (Claude API) e publicação (Meta Graph API)
- **Meta Graph API** — publicação direta no Instagram
- **Z-API** — notificações via WhatsApp

---

## Arquitetura

Layered Architecture com interfaces REST, DTOs e separação de responsabilidades:

```
com.north.producoes/
├── config/               # S3Config (AWS SDK)
├── controller/           # Implementações REST
│   ├── api/              # Interfaces OpenAPI documentadas
│   └── dto/
│       ├── request/      # DTOs de entrada
│       └── response/     # DTOs de saída
├── entity/               # Entidades JPA
│   └── enums/            # Enums (UserRoleEnum, PostStatusEnum...)
├── exception/            # GlobalExceptionHandler + exceções customizadas
├── repository/           # Spring Data JPA interfaces
├── security/             # JwtFilter, InternalApiKeyFilter, SecurityConfig, Auth
└── service/              # Regras de negócio
```

O frontend usa **Vite** com proxy dev para `http://localhost:8080`.

---

## Funcionalidades

- **Autenticação JWT** com refresh token — login, registro (admin), perfil (`/me`) e troca de senha
- **Controle de acesso por role** — `ADMIN` e `USER` com `@PreAuthorize` em endpoints sensíveis
- **Gestão de clientes** — CRUD completo com nome e dados de contato
- **Board de produção** — kanban com colunas: Demanda → Em Produção → Finalizado → Aguardando Aprovação → Agendado → Publicado
- **Upload de artes via S3** — presigned PUT URL gerada pelo backend; upload direto do browser ao bucket
- **Aprovação de artes** — registro com status, vinculado a cada post
- **Calendário editorial** — visão mensal de posts por cliente
- **Financeiro** — controle de transações associadas a clientes
- **Configurações de conta** — perfil, senha e integração Instagram Account ID por cliente (admin)
- **API interna para n8n** — protegida por `X-Internal-Api-Key`, retorna URL presigned de mídia + Instagram Account ID para publicação

---

## Fluxo principal

```
1. Post criado no board (status: DEMANDA)
2. Designer faz upload da arte → direto ao S3 via presigned URL
3. Registro de aprovação criado com a chave S3 (artS3Key)
4. n8n busca mídia via /api/internal/media-url/{postId}
5. n8n publica no Instagram usando Instagram Account ID configurado
6. n8n notifica o backend → status: PUBLICADO
7. Cliente aprovado pela equipe interna com um clique (aprovar/reprovar)
```

---

## Endpoints principais

| Prefixo | Descrição |
|---|---|
| `POST /api/auth/login` | Autenticação JWT |
| `POST /api/auth/refresh` | Refresh token |
| `GET/POST /api/users` | CRUD de usuários (admin) |
| `GET/PUT /api/users/me` | Perfil do usuário logado |
| `PUT /api/users/me/password` | Troca de senha |
| `GET/POST /api/clientes` | CRUD de clientes |
| `GET/POST /api/posts` | CRUD de posts |
| `PATCH /api/posts/{id}/status` | Atualiza status do post |
| `GET /api/posts/calendario` | Visão calendário |
| `GET/POST /api/aprovacoes` | CRUD de aprovações |
| `POST /api/aprovacoes/{id}/aprovar` | Aprova post |
| `POST /api/aprovacoes/{id}/reprovar` | Reprova post |
| `POST /api/media/upload-url` | Gera URL presigned para upload S3 |
| `GET /api/media/art-url` | Gera URL de preview da arte |
| `GET /api/internal/media-url/{postId}` | Endpoint n8n (API key) |
| `POST /api/admin/account-config` | Configura Instagram Account ID (admin) |
| `GET /v3/api-docs` | OpenAPI JSON |
| `GET /swagger-ui.html` | Swagger UI |

---

## Como rodar localmente

**Pré-requisitos:** Java 21, Maven (ou `mvnw`), Node 22+, Docker

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/north-producoes.git
cd north-producoes

# Suba o banco
docker compose up -d

# Rode o backend
./mvnw spring-boot:run

# Em outro terminal, rode o frontend
cd frontend_north
npm install
npm run dev
```

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`

---

## Variáveis de ambiente

Crie um arquivo `.env` na raiz do projeto:

```env
# Banco
DB_URL=jdbc:postgresql://localhost:5432/postgres
DB_USER=postgres
DB_PASSWORD=postgres

# JWT
JWT_KEY=base64_secret_aqui

# AWS S3
AWS_ACCESS_KEY_ID=sua_key
AWS_SECRET_ACCESS_KEY=seu_secret
AWS_S3_BUCKET=north-producoes-prod
AWS_S3_REGION=sa-east-1

# n8n
N8N_INTERNAL_KEY=sua_chave_interna
```

Valores entre `${}` no `application.properties` usam esses defaults ou fallbacks configurados.

---

## S3: Upload direto do browser

O fluxo de upload de artes funciona assim:

1. Frontend chama `POST /api/media/upload-url?clientId=&postId=&filename=&contentType=`
2. Backend gera **presigned PUT URL** com expiração de 15 min
3. Frontend faz `PUT` do arquivo **direto ao S3** usando a URL retornada
4. O backend nunca toca o binário — apenas orquestra a URL

CORS do bucket S3 deve permitir `PUT` e `OPTIONS` da origem do frontend.

---

## Scripts do frontend

| Comando | Ação |
|---|---|
| `npm run dev` | Servidor de desenvolvimento |
| `npm run build` | Build de produção |
| `npm run type-check` | Validação TypeScript |
| `npm run lint` | oxlint + eslint |
| `npm run format` | Prettier |
| `npm run test:unit` | Vitest |
| `npm run test:e2e` | Cypress |

---

## Licença

MIT
