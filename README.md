# North Produções — Sistema de Gestão de Conteúdo

Sistema web para agências de marketing gerenciarem clientes, produção de conteúdo, aprovação de artes e publicação no Instagram com automações integradas via n8n.

---

## Sobre o projeto

A North Produções operava com ferramentas desconectadas — Trello, Google Agenda, WhatsApp e planilhas separadas. Este sistema centraliza todo o fluxo em uma única plataforma: do briefing à publicação no Instagram, com geração automática de legendas via IA e aprovação de artes por link público sem necessidade de login.

---

## Tecnologias

**Backend**
- Java 21
- Spring Boot 4.0.4
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL

**Frontend**
- React
- Tailwind CSS
- shadcn/ui

**Automações**
- n8n (instância separada)
- Anthropic Claude API (geração de legendas)
- Meta Graph API (publicação Instagram)
- Google Calendar API
- Z-API (notificações WhatsApp)

---

## Arquitetura

Layered Architecture com quatro camadas:

```
com.north.producoes/
├── controller/        # Recebe requisições HTTP
├── service/           # Regras de negócio
├── repository/        # Acesso ao banco via JPA
├── model/             # Entidades e enums
├── dto/               # Request e Response objects
├── security/          # JWT Filter e Security Config
└── webhook/           # Recebimento de eventos do n8n
```

---

## Funcionalidades

- **Gestão de clientes** — cadastro com nicho, tom de voz e dados de contato
- **Board de produção** — kanban com colunas: Demanda → Em Produção → Finalizado → Aguardando Aprovação → Agendado → Publicado
- **Aprovação de artes** — link público enviado ao cliente, sem necessidade de criar conta
- **Geração de legendas** — automática via Claude API ao finalizar a arte
- **Calendário editorial** — visão mensal de todos os posts por cliente
- **Publicação automática** — agendamento via mLabs/Buffer após aprovação

---

## Fluxo principal

```
1. Post criado no board (status: DEMANDA)
2. Designer finaliza a arte (status: FINALIZADO)
3. n8n gera legenda via Claude API e cola no card
4. Status muda para AGUARDANDO_APROVACAO
5. Cliente recebe link e aprova com um clique
6. n8n envia para agendamento no mLabs/Buffer
7. Instagram publica no horário certo
8. n8n notifica o backend → status: PUBLICADO
```

---

## Endpoints principais

### Auth
```
POST /api/auth/login
POST /api/auth/refresh
```

### Clientes
```
GET    /api/clientes
POST   /api/clientes
GET    /api/clientes/{id}
PUT    /api/clientes/{id}
```

### Posts
```
GET    /api/posts?clienteId=&status=
POST   /api/posts
PATCH  /api/posts/{id}/status
GET    /api/posts/calendario
```

### Aprovação
```
GET    /api/aprovacoes/{token}        # público, sem auth
POST   /api/aprovacoes/{token}/aprovar
POST   /api/aprovacoes/{token}/reprovar
```

### Webhooks (n8n → sistema)
```
POST   /api/webhooks/n8n/caption-gerada
POST   /api/webhooks/n8n/publicado
```

---

## Como rodar localmente

**Pré-requisitos:** Java 21, Docker

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/north-producoes.git
cd north-producoes

# Suba o banco com Docker
docker compose up -d

# Rode a aplicação
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`.

---

## Variáveis de ambiente

```env
DB_URL=jdbc:postgresql://localhost:5432/northproducoes
DB_USERNAME=postgres
DB_PASSWORD=sua_senha

JWT_SECRET=seu_secret
JWT_EXPIRATION=86400000

N8N_WEBHOOK_SECRET=seu_secret
ANTHROPIC_API_KEY=sua_chave
```

---

## Roadmap

- [x] Arquitetura e modelagem de entidades
- [ ] Projeto Spring Boot — estrutura de pacotes
- [ ] Entidades JPA e migrations
- [ ] Autenticação JWT
- [ ] CRUD de clientes e posts
- [ ] Módulo de aprovação com link público
- [ ] Integração com n8n via webhooks
- [ ] Frontend React — board kanban
- [ ] Frontend React — calendário editorial
- [ ] Automações n8n completas

---

## Licença

MIT