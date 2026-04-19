# North Producoes Backend

Backend da plataforma North Producoes, voltada para organizacao operacional de uma agencia de marketing e producao de conteudo. A aplicacao centraliza autenticacao, gestao de usuarios, clientes, posts, aprovacoes, financeiro, configuracoes de conta e integracoes externas para automacao de publicacao.

O projeto foi estruturado para sustentar um fluxo de trabalho real de operacao: a demanda do post nasce no sistema, a arte pode ser enviada para o S3 por URL assinada, a legenda pode ser gerada com IA, a aprovacao fica registrada no backend e os dados finais podem ser consumidos por automacoes internas via n8n.

## Finalidade do projeto

O sistema existe para reduzir friccao operacional entre atendimento, criacao, aprovacao e publicacao. Em vez de espalhar informacoes entre planilhas, mensagens e ferramentas desconectadas, a aplicacao concentra o ciclo de vida do conteudo em um unico backend com regras de negocio, rastreabilidade e integracoes.

Casos de uso cobertos pelo backend:

- autenticacao e controle de acesso por perfil
- cadastro e manutencao de clientes
- organizacao de posts por status e agenda
- controle de artes e aprovacoes
- geracao assistida de legendas com IA
- suporte a publicacao por automacoes externas
- configuracao de contas vinculadas por cliente
- controle financeiro associado a clientes e operacao

## Principais capacidades

- Autenticacao com JWT e refresh token para operacao autenticada da plataforma.
- Organizacao de posts com vinculo a cliente e usuario responsavel.
- Registro de aprovacao por post, com arte, legenda e status.
- Upload e leitura de arte via AWS S3 com URLs presignadas.
- Integracao com Spring AI + OpenAI para geracao de legendas.
- Endpoint interno para n8n consumir dados de publicacao.
- Configuracao de Instagram Account ID por cliente para integracoes externas.
- Documentacao interativa via Swagger/OpenAPI.

## Arquitetura

O projeto segue uma arquitetura em camadas, com separacao clara entre contrato HTTP, orquestracao, regra de negocio, persistencia e integracoes.

```text
src/main/java/com/north/producoes
├── config/        Configuracoes de infraestrutura e integracoes
├── controller/    Controllers REST
│   ├── api/       Interfaces que definem contrato e documentacao OpenAPI
│   └── dto/       Objetos de request e response
├── entity/        Entidades JPA e enums de dominio
├── exception/     Tratamento global de erros e excecoes de dominio
├── repository/    Repositorios Spring Data JPA
├── security/      JWT, filtros, configuracao de seguranca e refresh token
└── service/       Regras de negocio e integracoes aplicacionais
```

Uma decisao importante do projeto e separar as interfaces REST na pasta `controller/api`. Esse padrao ajuda a concentrar rotas, contratos e anotacoes do Swagger em um unico lugar, deixando os controllers mais enxutos e facilitando a evolucao paralela do frontend.

## Stack tecnica

### Plataforma principal

- Java 21
- Spring Boot 4.0.4
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL

### Integracoes e suporte

- Spring AI 2.0.0-M4
- OpenAI
- AWS SDK v2 para S3 e presigner
- SpringDoc OpenAPI / Swagger UI
- JJWT 0.12.6
- Lombok

### Testes e apoio ao desenvolvimento

- H2 para testes
- Spring Boot test starters

## Modulos funcionais

### Autenticacao e seguranca

O backend usa JWT para autenticacao das requisicoes e refresh token para renovacao de sessao. Ha tambem um filtro interno para rotas consumidas por automacoes, protegido por chave dedicada via cabecalho `X-Internal-Api-Key`.

### Gestao de usuarios

Responsavel por administracao de usuarios, consulta do usuario autenticado, atualizacao de perfil e alteracao de senha.

### Gestao de clientes

Concentra os dados operacionais do cliente, como contato, nicho, tom de voz e configuracoes associadas ao atendimento e producao.

### Posts e aprovacoes

O post representa a demanda operacional. A aprovacao representa o estado final de arte e legenda vinculados ao post, permitindo rastrear status e preparar publicacao.

### Midia e S3

O sistema gera URLs presignadas para upload direto ao bucket e URLs temporarias para leitura de arte. Isso reduz carga no backend e evita trafego desnecessario de binarios pela aplicacao.

### IA para legenda

O backend integra com Spring AI e OpenAI para gerar legendas com base em contexto do post, imagem e identidade de marca do cliente.

### Integracao com n8n

Ha rotas internas para fornecer ao n8n os dados necessarios ao fluxo de publicacao, incluindo URL da midia, legenda e conta de destino configurada.

### Financeiro

Modulo de controle financeiro vinculado a clientes e usuarios, pensado para apoiar o acompanhamento operacional da agencia.

## Fluxo operacional resumido

```text
1. Um post e criado e associado a um cliente e a um usuario responsavel
2. A arte pode ser enviada ao S3 por meio de URL presignada
3. A legenda pode ser gerada com IA com base na imagem e no contexto do cliente
4. A aprovacao do material e registrada no backend
5. O n8n pode consumir os dados internos de midia e publicacao
6. A operacao acompanha o status do conteudo dentro da plataforma
```

## Banco de dados e migracoes

O esquema da aplicacao e controlado por Flyway. As migracoes ficam em:

```text
src/main/resources/db/migration
```

Esse modelo evita drift de estrutura entre ambientes e torna a evolucao do banco versionada junto com o codigo.

## Executando o projeto localmente

### Pre-requisitos

- Java 21
- Maven ou `./mvnw`
- PostgreSQL

### Subindo a aplicacao

```bash
./mvnw spring-boot:run
```

Por padrao, a aplicacao sobe na porta `8080`.

Se o ambiente estiver configurado para uso de Docker Compose pelo Spring Boot, o suporte de runtime ja esta presente no projeto.

## Variaveis de ambiente

As configuracoes principais do projeto sao resolvidas a partir de variaveis de ambiente.

```env
DB_URL=jdbc:postgresql://localhost:5432/producoes
DB_USER=postgres
DB_PASSWORD=postgres

JWT_KEY=base64_secret

AWS_S3_BUCKET=nome-do-bucket
AWS_S3_REGION=sa-east-1
AWS_ACCESS_KEY_ID=sua-access-key
AWS_SECRET_ACCESS_KEY=sua-secret-key

N8N_INTERNAL_KEY=chave-interna-forte

OPENAI_API_KEY=sua-chave-openai
OPENAI_MODEL=gpt-4o
```

## Documentacao da API

Os endpoints da API sao documentados via Swagger/OpenAPI. A referencia interativa fica disponivel em ambiente local por meio do Swagger UI, e o contrato OpenAPI pode ser consumido diretamente pelo frontend ou por ferramentas de integracao.

O README nao replica a lista de endpoints para evitar duplicacao de documentacao e drift de contrato.

## Seguranca e observacoes de implementacao

- As rotas autenticadas usam JWT.
- As rotas internas de automacao usam chave dedicada.
- O backend utiliza DTOs de request e response para isolar contrato HTTP das entidades JPA.
- URLs de S3 sao temporarias e geradas sob demanda.
- O projeto foi organizado para evoluir em conjunto com frontend, Swagger e automacoes externas sem concentrar toda a logica nos controllers.

## Estrutura para evolucao

O projeto ja possui base adequada para crescer com:

- fortalecimento progressivo das regras de autorizacao
- ampliacao de validacoes por DTO
- novos fluxos de aprovacao e publicacao
- integracoes adicionais com ferramentas de operacao
- evolucao dos modulos de IA, financeiro e auditoria

## Licenca

Uso interno / privado, salvo definicao diferente pelo mantenedor do projeto.
