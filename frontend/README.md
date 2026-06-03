# North Produções — Frontend

Interface web do sistema North Produções, plataforma de gestão operacional para produtora audiovisual.

---

## Stack

| Tecnologia | Versão |
|---|---|
| Vue 3 + TypeScript | 3.5 |
| Vite | 8 |
| Tailwind CSS | 3.4 |
| Vue Router | 5 |
| Pinia | 3 |
| Lucide Vue Next | — |
| vuedraggable (kanban) | 4 |
| Zod (validação) | 4 |
| oxlint + eslint + prettier | — |
| Vitest + Cypress | — |

Componentes UI construídos sobre **Tailwind CSS** + **class-variance-authority**, seguindo o padrão shadcn-vue.

---

## Funcionalidades

- **Login** — autenticação JWT com refresh token e persistência de sessão
- **Dashboard** — métricas de produção, dados de clientes e tabela de demandas recentes
- **Board Kanban** — drag-and-drop de posts entre colunas: Demanda, Em Produção, Finalizado, Aguardando Aprovação, Agendado, Publicado; upload de arte direto ao S3
- **Calendário** — visão mensal de posts com filtro por cliente
- **Clientes** — cadastro, edição, exclusão e link rápido para board do cliente
- **Aprovações** — lista de artes aguardando aprovação
- **Financeiro** — cadastro e controle de transações por cliente
- **Configurações** — perfil do usuário, troca de senha, vinculação de conta Instagram via Meta e grupo de WhatsApp por cliente (admin)
- **Usuários** — listagem, criação, alteração de papel e exclusão de colaboradores (admin)

---

## Estrutura

```
src/
├── assets/           # Imagens e CSS global
├── components/
│   ├── layout/       # AppLayout, Sidebar, Topbar
│   └── ui/           # Avatar, Badge, Button, Card, Input
├── lib/
│   ├── api.ts        # Cliente HTTP base (fetch + JWT handling)
│   └── utils.ts      # cn() utilitário para classes
├── router/           # Rotas e guardas de autenticação
├── services/         # Serviços tipados por domínio (post, client, finance, approval, media, account, user)
└── views/            # Páginas da aplicação
```

---

## Requisitos

- Node `^20.19.0 || >=22.12.0`
- Backend rodando em `http://localhost:8080` (ou configure via `VITE_API_BASE_URL`)

---

## Configuração

Crie um arquivo `.env` na raiz do projeto se o backend estiver em outra URL:

```env
VITE_API_BASE_URL=http://localhost:8080
```

---

## Scripts

```bash
# Instalar dependências
npm install

# Servidor de desenvolvimento (hot reload)
npm run dev

# Build para produção
npm run build
# Inclui validação de tipos (vue-tsc)

# Verificar tipos
npm run type-check

# Lint (oxlint + eslint)
npm run lint

# Formatar código
npm run format

# Testes unitários
npm run test:unit

# Testes E2E (headless)
npm run test:e2e

# Testes E2E (modo interativo)
npm run test:e2e:dev
```

---

## Serviço por serviço

| Serviço | Métodos exportados |
|---|---|
| `postService` | `getAll`, `create`, `updateStatus` |
| `clientService` | `getAll`, `create`, `update`, `delete` |
| `financeService` | `getAllByClient`, `create`, `delete` |
| `approvalService` | `create`, `getAll`, `approve`, `reject`, `delete` |
| `mediaService` | `getUploadUrl`, `uploadToS3`, `getArtPreviewUrl` |
| `accountConfigService` | `configure`, `getByClientId` |
| `userService` | `getMe`, `updateProfile`, `changePassword`, `listAll`, `create`, `updateRole`, `deleteById` |

Todos os serviços utilizam `apiFetch` em `src/lib/api.ts`, que:

- Anexa `Authorization: Bearer <token>` automaticamente
- Faz logout se receber 401
- Mostra mensagem de "Acesso negado" se receber 403
- Retorna `undefined` para 204 No Content

---

## Autenticação

O fluxo de login guarda o token em `localStorage` ou `sessionStorage` (conforme checkbox "Lembrar-me"). O `apiFetch` injeta o token em todas as requisições e redireciona para `/login` em caso de 401.

O roteador (`src/router/index.ts`) protege todas as rotas exceto `/login` e aplica `requiresAdmin` quando necessário.

---

## Licença

MIT
