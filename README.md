# North Producoes Backend

Backend da plataforma **North Producoes**, um sistema para gestao operacional de uma agencia de marketing e producao de conteudo. A aplicacao centraliza autenticacao, clientes, usuarios, posts, aprovacoes, midias, financeiro e integracoes externas em uma API Spring Boot preparada para operar junto com frontend, automacoes n8n, AWS S3, Meta Graph API, Evolution API e OpenAI.

O objetivo do projeto e reduzir a dependencia de planilhas, mensagens soltas e controles manuais, mantendo o ciclo de vida da producao de conteudo em um backend unico, versionado, testavel e com regras de negocio concentradas no servidor.

## Sumario

- [Visao do produto](#visao-do-produto)
- [Principais capacidades](#principais-capacidades)
- [Arquitetura](#arquitetura)
- [Stack tecnica](#stack-tecnica)
- [Modulos funcionais](#modulos-funcionais)
- [Fluxo operacional](#fluxo-operacional)
- [Mapa de endpoints](#mapa-de-endpoints)
- [Seguranca](#seguranca)
- [Integracoes externas](#integracoes-externas)
- [Banco de dados e migracoes](#banco-de-dados-e-migracoes)
- [Executando localmente](#executando-localmente)
- [Variaveis de ambiente](#variaveis-de-ambiente)
- [Testes e qualidade](#testes-e-qualidade)
- [Padroes de desenvolvimento](#padroes-de-desenvolvimento)
- [Troubleshooting](#troubleshooting)

## Visao do produto

A North Producoes opera um fluxo em que cada cliente pode ter demandas de conteudo, artes, legendas, status de aprovacao, dados de publicacao, contatos de WhatsApp, configuracoes de Instagram e informacoes financeiras. Este backend foi construido para ser a fonte de verdade desse fluxo.

Em termos praticos, a API permite:

- registrar clientes e manter dados operacionais de atendimento;
- criar posts vinculados a cliente e usuario responsavel;
- controlar status de producao, aprovacao e publicacao;
- gerar URLs presignadas para upload de artes no S3;
- confirmar upload de midia e disparar webhook para automacao;
- gerar legenda com IA usando contexto do post e do cliente;
- disponibilizar dados internos para n8n publicar conteudo;
- vincular contas Instagram via Meta Graph API;
- consultar e vincular grupos do WhatsApp via Evolution API;
- controlar lancamentos financeiros associados a clientes;
- proteger operacoes administrativas por perfil e rotas internas por API key.

## Principais capacidades

| Area | Capacidade |
| --- | --- |
| Autenticacao | Login, registro, JWT, refresh token e usuario autenticado |
| Usuarios | Cadastro, consulta, atualizacao de perfil, troca de senha e rotas administrativas |
| Clientes | Cadastro, consulta por email/numero/status, atualizacao, exclusao e vinculo com grupo WhatsApp |
| Posts | Criacao, edicao, filtros por status/cliente/usuario/data e geracao de legenda |
| Aprovacoes | Registro de arte/legenda, aprovacao, rejeicao, atualizacao de status e callbacks internos |
| Midia | Upload direto ao S3 por URL presignada, preview temporario e URL interna para automacoes |
| Financeiro | Criacao, consulta, atualizacao e exclusao de registros financeiros |
| Meta | Consulta e vinculo de contas Instagram Business por cliente |
| Evolution API | Consulta de grupos e vinculo de grupo WhatsApp ao cliente |
| n8n | Webhook de upload completo e rota interna protegida por `X-Internal-Api-Key` |
| Observabilidade | Actuator e Qodana configurados para apoio de qualidade |
| Documentacao | Swagger/OpenAPI via SpringDoc |

## Arquitetura

O projeto segue arquitetura em camadas, com separacao entre contrato HTTP, regras de negocio, persistencia, seguranca e integracoes externas.

```text
src/main/java/com/north/producoes
├── config/                 Configuracoes de infraestrutura e clientes externos
├── controller/             Implementacoes REST
│   ├── api/                Interfaces de contrato e documentacao OpenAPI
│   └── dto/                DTOs de request e response
├── entity/                 Entidades JPA e enums de dominio
├── exception/              Excecoes de dominio e handler global
├── integration/            Clientes e DTOs de APIs externas
│   ├── evolutionApi/       Cliente Evolution API e modelos externos
│   └── meta/               Cliente Meta Graph API e modelos externos
├── repository/             Repositorios Spring Data JPA
├── security/               JWT, filtros, refresh token e configuracao de seguranca
└── service/                Regras de negocio, orquestracao e integracoes aplicacionais
```

### Decisoes arquiteturais importantes

- **Controllers enxutos**: os controllers delegam regra de negocio para services.
- **Contratos separados em `controller/api`**: rotas, operacoes e documentacao OpenAPI ficam em interfaces dedicadas.
- **DTOs na borda da API**: requests e responses nao expõem diretamente a estrutura interna das entidades.
- **Integracoes externas isoladas**: clientes de Meta Graph e Evolution API ficam em pacotes proprios, com DTOs externos separados dos DTOs da aplicacao.
- **Regras no backend**: autorizacao, validacao, mudanca de status e composicao de payloads ficam no servidor.
- **Banco versionado por Flyway**: mudancas estruturais sao aplicadas por migracoes controladas.
- **Testes por camada**: services com Mockito/JUnit e controllers com testes focados no contrato HTTP.

## Stack tecnica

### Plataforma

- Java 21
- Spring Boot 4.0.4
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Hibernate
- Flyway
- PostgreSQL
- Maven Wrapper

### Integracoes

- AWS SDK v2 para S3 e presigned URLs
- Spring AI 2.0.0-M4
- OpenAI
- Meta Graph API
- Evolution API
- n8n
- SpringDoc OpenAPI / Swagger UI

### Dependencias de apoio

- JJWT 0.13.0
- Lombok
- H2 para testes
- AssertJ
- Mockito
- JUnit 5
- Qodana

## Modulos funcionais

### Autenticacao e sessao

Responsavel por autenticar usuarios, emitir tokens JWT e renovar sessao com refresh token.

Principais responsabilidades:

- autenticar por email e senha;
- registrar usuarios;
- gerar token de acesso;
- renovar token por refresh token;
- aplicar criptografia de senha com BCrypt;
- carregar usuario autenticado pelo filtro JWT.

### Usuarios

Modulo de administracao e autocuidado do usuario.

Recursos principais:

- listagem e busca por email;
- criacao administrativa;
- exclusao administrativa;
- consulta de perfil autenticado;
- atualizacao de dados pessoais;
- troca de senha.

Rotas administrativas usam `@PreAuthorize` para restringir acesso a usuarios com perfil administrativo.

### Clientes

Representa a conta operacional atendida pela agencia.

Campos relevantes:

- nome;
- email;
- telefone;
- link do Drive;
- nicho;
- tom de voz;
- status;
- grupo WhatsApp vinculado pela Evolution API.

O cliente e a base para posts, aprovacoes, midias, conta Instagram e financeiro.

### Posts

Representa uma demanda de conteudo. Cada post possui:

- titulo;
- tema;
- objetivo;
- status;
- data agendada;
- cliente;
- usuario responsavel;
- aprovacao associada.

O post concentra o planejamento da peca antes da aprovacao e publicacao.

### Aprovacoes

A aprovacao registra o material que sera analisado e potencialmente publicado.

Ela guarda:

- chave da arte no S3;
- nome da arte;
- legenda;
- status de aprovacao;
- data de aprovacao;
- usuario aprovador;
- dados de envio pelo WhatsApp quando retornados pela automacao.

### Midia e S3

O backend nao precisa receber arquivos pesados diretamente. Em vez disso:

1. O frontend solicita uma URL presignada.
2. O arquivo e enviado diretamente ao S3.
3. O frontend confirma o upload.
4. O backend atualiza o estado da aprovacao/post.
5. O backend pode disparar webhook para o n8n.
6. O n8n consulta uma rota interna para obter a URL e os dados de publicacao.

Esse modelo reduz trafego no backend e melhora escalabilidade.

### IA para legendas

O `OpenAiService` usa Spring AI para gerar legendas considerando:

- titulo do post;
- tema;
- objetivo;
- contexto do cliente;
- tom de voz;
- imagem, quando uma URL estiver disponivel.

Falhas de integracao sao convertidas para excecoes de dominio, evitando que detalhes da API externa vazem diretamente para o contrato HTTP.

### Conta Instagram e Meta Graph

O modulo Meta consulta paginas e contas Instagram Business disponiveis a partir do token configurado. Depois, permite vincular uma conta ao cliente.

O vinculo final guarda:

- `clientId`;
- `igUserId`;
- `accessToken`;
- usuario administrador que realizou a configuracao.

### Evolution API e WhatsApp

A integracao com Evolution API permite:

- consultar grupos disponiveis;
- vincular `whatsappGroupId` e `whatsappGroupName` ao cliente;
- preparar o backend para fluxos de envio e rastreio de mensagem via n8n.

### Financeiro

Modulo para controle financeiro associado a clientes.

Permite:

- listar registros;
- filtrar por status;
- buscar por cliente;
- criar lancamento;
- atualizar lancamento;
- excluir lancamento.

Atualmente o controller financeiro e protegido para usuarios administradores.

## Fluxo operacional

```text
1. Usuario autentica na plataforma.
2. Cliente e cadastrado com dados operacionais.
3. Post e criado para o cliente.
4. Arte e enviada ao S3 via URL presignada.
5. Upload e confirmado no backend.
6. Backend atualiza status e pode disparar webhook n8n.
7. Legenda pode ser gerada com IA.
8. Material e aprovado ou rejeitado.
9. n8n consulta dados internos para publicacao.
10. Backend recebe/guarda dados de retorno da automacao.
```

## Mapa de endpoints

> O Swagger e a fonte interativa do contrato. Este mapa serve como visao rapida para desenvolvimento.

### Autenticacao

Base: `/api/auth`

| Metodo | Rota | Descricao |
| --- | --- | --- |
| POST | `/login` | Autentica usuario e retorna tokens |
| POST | `/register` | Registra usuario |
| POST | `/refresh` | Renova token de acesso |

### Usuarios

Base: `/api/users`

| Metodo | Rota | Descricao |
| --- | --- | --- |
| GET | `/` | Lista usuarios |
| GET | `/email/{email}` | Busca usuario por email |
| POST | `/` | Cria usuario |
| DELETE | `/id/{id}` | Remove usuario |
| GET | `/me` | Retorna usuario autenticado |
| PUT | `/me` | Atualiza perfil autenticado |
| PUT | `/me/password` | Altera senha do usuario autenticado |

### Clientes

Base: `/api/clients`

| Metodo | Rota | Descricao |
| --- | --- | --- |
| GET | `/` | Lista clientes |
| GET | `/email/{email}` | Busca por email |
| GET | `/number/{number}` | Busca por telefone |
| GET | `/status/{status}` | Filtra por status |
| POST | `/` | Cria cliente |
| PUT | `/update/{id}` | Atualiza cliente |
| DELETE | `/id/{id}` | Remove cliente |

### Posts

Base: `/api/posts`

| Metodo | Rota | Descricao |
| --- | --- | --- |
| GET | `/` | Lista posts |
| GET | `/status/{status}` | Filtra por status |
| GET | `/client/{id}` | Lista posts por cliente |
| GET | `/user/{id}` | Lista posts por usuario |
| GET | `/scheduled/{scheduledAt}` | Busca por data agendada |
| POST | `/save` | Cria post |
| PUT | `/update/{id}` | Atualiza post |
| DELETE | `/delete/{id}` | Remove post |
| POST | `/{id}/generate-caption` | Gera legenda com IA |

### Aprovacoes

Base: `/api/post-approvals`

| Metodo | Rota | Descricao |
| --- | --- | --- |
| GET | `/all` | Lista aprovacoes |
| GET | `/{id}` | Busca aprovacao por ID |
| GET | `/status/{status}` | Filtra por status |
| POST | `/approve/{id}` | Aprova material |
| POST | `/reject/{id}` | Rejeita material |
| POST | `/save` | Cria aprovacao |
| PUT | `/update/{id}` | Atualiza aprovacao |
| DELETE | `/delete/{id}` | Remove aprovacao |

### Rotas internas de aprovacao

Base: `/api/internal/post-approvals`

| Metodo | Rota | Descricao |
| --- | --- | --- |
| PATCH | `/{id}/whatsapp` | Atualiza dados de envio WhatsApp |
| PATCH | `/{id}/status` | Atualiza status por automacao |
| GET | `/whatsapp` | Lista aprovacoes relevantes para WhatsApp |

Essas rotas nao usam JWT. Elas sao protegidas pelo header `X-Internal-Api-Key`.

### Midia

| Metodo | Rota | Descricao |
| --- | --- | --- |
| POST | `/api/media/upload-url` | Gera URL presignada de upload |
| POST | `/api/media/upload-complete` | Confirma upload e dispara fluxo posterior |
| GET | `/api/media/art-url` | Retorna URL temporaria de preview |
| GET | `/api/internal/media-url/{postId}` | Retorna dados de midia para n8n |

### Financeiro

Base: `/api/finance`

| Metodo | Rota | Descricao |
| --- | --- | --- |
| GET | `/` | Lista lancamentos |
| GET | `/status/{status}` | Filtra por status |
| GET | `/client/{id}` | Busca por cliente |
| POST | `/create` | Cria lancamento |
| PATCH | `/update/{id}` | Atualiza lancamento |
| DELETE | `/delete/{id}` | Remove lancamento |

### Configuracao de contas

Base: `/api/admin/account-config`

| Metodo | Rota | Descricao |
| --- | --- | --- |
| POST | `/` | Configura conta externa para cliente |
| GET | `/client/{clientId}` | Busca configuracao por cliente |

### Meta Graph

Base: `/api/admin/meta`

| Metodo | Rota | Descricao |
| --- | --- | --- |
| GET | `/instagram-accounts` | Lista contas Instagram Business disponiveis |
| POST | `/instagram-accounts/link` | Vincula conta Instagram a um cliente |

### Evolution API

Base: `/api/admin/evolution`

| Metodo | Rota | Descricao |
| --- | --- | --- |
| GET | `/groups` | Lista grupos da instancia Evolution |
| POST | `/groups/link` | Vincula grupo WhatsApp a cliente |

## Seguranca

O backend usa uma estrategia stateless:

- JWT no header `Authorization: Bearer <token>`;
- refresh token para renovacao;
- BCrypt para hash de senha;
- `@PreAuthorize` em rotas administrativas;
- `X-Internal-Api-Key` para rotas internas consumidas pelo n8n;
- CSRF desabilitado por se tratar de API stateless;
- CORS configurado para o frontend local em `http://localhost:5173`;
- Swagger desabilitado por padrao no perfil base/producao.

### Politica de exposicao

As rotas `/api/auth/**` sao publicas. As rotas `/api/internal/**` nao usam JWT, mas passam pelo `InternalApiKeyFilter`. Todas as demais rotas exigem autenticacao e podem ter restricoes adicionais por perfil.

### Cuidados operacionais

- Nao commitar `.env` com segredos reais.
- Usar `JWT_KEY` forte e adequado para o algoritmo de assinatura.
- Definir `N8N_INTERNAL_KEY` em qualquer ambiente que use rotas internas.
- Desabilitar Swagger em producao, como ja configurado em `application-prod.properties`.
- Evitar expor URLs permanentes de arquivos sensiveis; o padrao do projeto e gerar URLs sob demanda.

## Integracoes externas

### AWS S3

Usado para armazenamento de artes e geracao de URLs presignadas.

Configuracoes principais:

- `AWS_S3_BUCKET`
- `AWS_S3_REGION`
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_S3_PUBLIC_PREFIX`
- `AWS_S3_PUBLIC_BASE_URL`

### OpenAI

Usado via Spring AI para geracao de legendas.

Configuracoes:

- `OPENAI_API_KEY`
- `OPENAI_MODEL`

Modelo padrao: `gpt-4o`.

### n8n

Usado para automacoes internas.

Configuracoes:

- `N8N_INTERNAL_KEY`: protege rotas internas do backend.
- `N8N_WEBHOOK_UPLOAD_COMPLETE_URL`: webhook chamado quando upload e confirmado.
- `N8N_WEBHOOK_API_KEY`: chave enviada ao webhook configurado.

### Meta Graph API

Usada para listar e vincular contas Instagram Business.

Configuracoes:

- `META_GRAPH_BASE_URL`
- `META_GRAPH_API_VERSION`
- `META_GRAPH_ACCESS_TOKEN`

### Evolution API

Usada para consultar grupos do WhatsApp e vincular grupo ao cliente.

Configuracoes:

- `EVOLUTION_API_BASE_URL`
- `EVOLUTION_API_KEY`
- `EVOLUTION_API_INSTANCE`

## Banco de dados e migracoes

O banco principal e PostgreSQL. O schema e controlado por Flyway em:

```text
src/main/resources/db/migration
```

Configuracao relevante:

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
spring.jpa.hibernate.ddl-auto=validate
```

O `ddl-auto=validate` e intencional: a aplicacao valida se o banco esta aderente as entidades, mas nao altera schema automaticamente. Mudancas estruturais devem ser feitas por migracoes.

## Executando localmente

### Pre-requisitos

- Java 21
- Maven Wrapper do projeto (`./mvnw`)
- PostgreSQL local ou via Docker Compose
- Variaveis de ambiente configuradas

### Subir PostgreSQL com Docker Compose

```bash
docker compose up -d
```

O `compose.yaml` disponibiliza PostgreSQL na porta `5432`.

### Configurar ambiente

Crie um `.env` local a partir de `.env_example` ou exporte as variaveis no terminal. O projeto le `application.properties` e espera as principais variaveis obrigatorias do ambiente.

Exemplo minimo para desenvolvimento:

```env
DB_URL=jdbc:postgresql://localhost:5432/postgres
DB_USER=postgres
DB_PASSWORD=postgres
JWT_KEY=defina-uma-chave-forte-local
SPRING_PROFILES_ACTIVE=dev
```

Para executar com perfil de desenvolvimento:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Aplicacao local:

```text
http://localhost:8080
```

Swagger em desenvolvimento:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Variaveis de ambiente

| Variavel | Obrigatoria | Uso |
| --- | --- | --- |
| `DB_URL` | Sim | URL JDBC do PostgreSQL |
| `DB_USER` | Sim | Usuario do banco |
| `DB_PASSWORD` | Sim | Senha do banco |
| `JWT_KEY` | Sim | Segredo usado para JWT |
| `SPRING_PROFILES_ACTIVE` | Recomendado | Perfil ativo, como `dev` ou `prod` |
| `AWS_S3_BUCKET` | Para S3 | Bucket de artes |
| `AWS_S3_REGION` | Para S3 | Regiao AWS |
| `AWS_ACCESS_KEY_ID` | Para S3 | Access key |
| `AWS_SECRET_ACCESS_KEY` | Para S3 | Secret key |
| `AWS_S3_PUBLIC_PREFIX` | Opcional | Prefixo publico, padrao `public/posts` |
| `AWS_S3_PUBLIC_BASE_URL` | Opcional | Base publica quando aplicavel |
| `OPENAI_API_KEY` | Para IA | Chave OpenAI |
| `OPENAI_MODEL` | Opcional | Modelo de chat |
| `N8N_INTERNAL_KEY` | Para rotas internas | API key exigida em `/api/internal/**` |
| `N8N_WEBHOOK_UPLOAD_COMPLETE_URL` | Para webhook | URL chamada ao concluir upload |
| `N8N_WEBHOOK_API_KEY` | Para webhook | Chave enviada ao n8n |
| `META_GRAPH_BASE_URL` | Opcional | Base URL da Meta Graph API |
| `META_GRAPH_API_VERSION` | Opcional | Versao da API, padrao `v19.0` |
| `META_GRAPH_ACCESS_TOKEN` | Para Meta | Token da Meta |
| `EVOLUTION_API_BASE_URL` | Para Evolution | URL da Evolution API |
| `EVOLUTION_API_KEY` | Para Evolution | Chave da Evolution API |
| `EVOLUTION_API_INSTANCE` | Para Evolution | Instancia usada nas chamadas |

## Testes e qualidade

O projeto possui cobertura unitária e de controllers usando JUnit 5, Mockito, AssertJ e recursos de teste do Spring Boot.

### Rodar todos os testes

```bash
./mvnw test
```

Para manter alinhamento com o projeto, use Java 21:

```bash
JAVA_HOME=/caminho/para/jdk-21 ./mvnw test
```

### Estrutura dos testes

```text
src/test/java/com/north/producoes
├── controller/    Testes de contrato HTTP/controller
├── security/      Testes de autenticacao
└── service/       Testes unitarios de regra de negocio
```

### Padrao recomendado

- Services: JUnit 5 + Mockito, sem subir contexto Spring.
- Controllers: testes focados em status HTTP, payload e delegacao.
- Assertions: AssertJ para legibilidade.
- Estrutura: Arrange, Act, Assert.
- Dependencias externas: sempre mockadas em testes unitarios.
- Banco real: deixar para testes de integracao dedicados.

### Qodana

O projeto possui `qodana.yaml` com:

- linter JVM 2026.1;
- JDK 21;
- profile recomendado;
- checagem de licencas de dependencias.

## Padroes de desenvolvimento

### Maven

Use sempre o Maven Wrapper:

```bash
./mvnw <comando>
```

Isso reduz diferencas entre ambientes locais e CI.

### Git

O `AGENTS.md` do projeto define:

- mudancas focadas no escopo solicitado;
- branches para alteracoes nao triviais;
- commits no padrao Conventional Commits;
- testes relevantes antes de concluir;
- evitar rewrites especulativos.

Exemplos de mensagens:

```text
feat: add media upload workflow
fix: update vulnerable dependencies
test: cover post service caption generation
docs: improve backend README
```

### API

Ao criar nova funcionalidade:

1. Defina DTOs de request/response.
2. Exponha o contrato em `controller/api`.
3. Implemente controller enxuto.
4. Coloque regra de negocio no service.
5. Persista via repository.
6. Mapeie excecoes de dominio no handler global quando necessario.
7. Adicione testes focados no comportamento.

### Integracoes

Para novas APIs externas:

- criar `Config` para propriedades;
- criar client dedicado em `integration/<provedor>`;
- separar DTO externo de DTO interno;
- converter falhas para excecoes de dominio;
- testar service com mocks;
- evitar expor resposta bruta externa no contrato da aplicacao.

## Troubleshooting

### Swagger nao abre

No perfil base e em producao o Swagger fica desabilitado. Use:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Depois acesse:

```text
http://localhost:8080/swagger-ui.html
```

### Erro de JWT secret

Confirme se `JWT_KEY` esta definido. Em ambiente real, use um segredo forte e gerenciado por mecanismo seguro de configuracao.

### Rotas internas retornam 401 ou 503

Rotas `/api/internal/**` exigem:

```http
X-Internal-Api-Key: <valor de N8N_INTERNAL_KEY>
```

Se `N8N_INTERNAL_KEY` nao estiver configurado, o backend responde `503`.

### Testes com warning do Mockito

O warning de auto-attach do Mockito/Byte Buddy pode aparecer em JDKs modernos. Ele nao significa falha se o processo terminar com `BUILD SUCCESS`. Para este projeto, mantenha a execucao em JDK 21.

### Hibernate reclama de schema

Como `spring.jpa.hibernate.ddl-auto=validate`, divergencias entre entidade e banco quebram a inicializacao. Corrija com migracao Flyway em `src/main/resources/db/migration`.

### Dependencias vulneraveis

Fluxo recomendado:

1. Identificar a dependencia e o caminho transitorio com `./mvnw dependency:tree`.
2. Se for gerenciada pelo Spring Boot, preferir atualizar a versao do parent quando for seguro.
3. Se for dependencia direta, atualizar a versao no `pom.xml`.
4. Se for transitiva, atualizar a dependencia raiz antes de usar exclusoes.
5. Rodar `./mvnw test`.

## Licenca

Projeto privado/interno da North Producoes, salvo definicao diferente pelo mantenedor.
