# InvestAlert API

Backend para monitoramento de oportunidades de investimento em FIIs (Fundos de Investimento Imobiliario). Permite criar regras de monitoramento sobre ativos, receber alertas automaticos quando as condicoes sao atendidas e consultar historico de alertas.

## Tecnologias

- Java 25
- Spring Boot 3.5
- Spring Security + JWT (jjwt 0.12.6)
- Spring Data JPA / Hibernate
- MySQL 8.4
- SpringDoc OpenAPI (Swagger UI)
- Lombok
- jqwik (property-based testing)
- Docker Compose

## Arquitetura

O projeto segue Clean Architecture com as seguintes camadas:

```
domain          - Entidades (User, Role, Permission, Asset, Rule, RuleGroup, Alert),
                  enumeradores (SubscriptionPlan, AlertStatus, RuleField, ComparisonOperator),
                  ports/out (repositorios, PasswordEncoder, TokenProvider)
application     - ports/in (interfaces de use cases), use cases (implementacoes),
                  commands, responses
adapters        - Controllers REST (web/v1), adaptadores JPA (persistence)
infrastructure  - Configuracoes Spring (Security, JWT, OpenAPI, versionamento)
```

A camada `domain` e completamente livre de dependencias externas. As interfaces de use case (`ports/in`) vivem em `application` junto com os commands e responses que definem seus contratos.

As entidades JPA (`adapters/persistence/entities`) utilizam o padrao Builder via Lombok, e os mappers constroem essas entidades exclusivamente via builder.

### Fluxo de dependencias

```
adapters/web  ->  application.ports.in  <-  application.usecases
                                                     |
                                            domain.ports.out
                                                     |
                                          adapters/persistence
```

### Fluxo principal

```
HTTP Request
    -> JwtAuthenticationFilter (valida token, extrai permissoes do claim JWT)
        -> Controller (v1) (@PreAuthorize verifica authority)
            -> Use Case (application.ports.in)
                -> Domain (regras de negocio, ownership check)
                -> Repository (persistencia MySQL)
```

## Autorizacao (RBAC)

O sistema utiliza Role-Based Access Control (RBAC) com o modelo `usuario -> roles -> permissoes`.

### Roles e permissoes

| Role         | Permissoes                                                    |
|--------------|---------------------------------------------------------------|
| `ROLE_ADMIN` | `ALERT_CREATE`, `ALERT_UPDATE`, `ALERT_DELETE`, `USER_MANAGE`, `SYSTEM_CONFIG` |
| `ROLE_USER`  | `ALERT_CREATE`, `ALERT_UPDATE`, `ALERT_DELETE`                |

### Protecao dos endpoints

| Endpoint                  | Permissao exigida  |
|---------------------------|--------------------|
| `POST /api/v1/rules`      | `ALERT_CREATE`     |
| `PUT /api/v1/rules/{id}`  | `ALERT_UPDATE`     |
| `DELETE /api/v1/rules/{id}` | `ALERT_DELETE`   |
| Demais endpoints autenticados | `isAuthenticated()` |

As permissoes sao embutidas no JWT como claim `permissions` no momento do login e extraidas pelo `JwtAuthenticationFilter` a cada requisicao, sem consulta adicional ao banco.

### Ownership

Operacoes de mutacao (`PUT`, `DELETE`) em regras verificam se o recurso pertence ao usuario autenticado na camada de use case. Recurso inexistente retorna `404`; recurso de outro usuario retorna `403`.

### Plano de assinatura

O campo `subscriptionPlan` (`FREE`, `PREMIUM`, `PRO`) e um atributo do usuario, separado do modelo de autorizacao. Nao e usado em `GrantedAuthority` e nao influencia o RBAC.

## Pre-requisitos

- Java 25+
- Docker e Docker Compose
- Maven 3.9+ (ou use o wrapper `./mvnw`)

## Como rodar (standalone)

### 1. Subir o banco de dados

```bash
docker compose up -d
```

Isso inicia o **MySQL 8.4** na porta `3306` com schema e dados de seed carregados automaticamente.

### 2. Iniciar a aplicacao

```bash
./mvnw spring-boot:run
```

A API estara disponivel em `http://localhost:8080`.

> O schema e gerenciado pelo Spring via `src/main/resources/schema.sql` (`spring.sql.init.mode: always`). O Hibernate valida o schema na inicializacao (`ddl-auto: validate`).

## Banco de dados

Os scripts de inicializacao sao executados automaticamente na primeira vez que o container MySQL e criado:

| Arquivo                                           | Descricao                                          |
|---------------------------------------------------|----------------------------------------------------|
| `src/main/resources/schema.sql`                   | DDL completo (tabelas, indices, FKs)               |
| `docker/mysql/init/01-seed-roles-permissions.sql` | Roles, permissoes e atribuicoes                    |
| `docker/mysql/init/02-seed-assets.sql`            | Ativos FII para testes                             |
| `docker/mysql/init/03-seed-demo-user.sql`         | Usuario demo com `ROLE_USER`                       |
| `docker/mysql/init/04-seed-admin-user.sql`        | Usuario admin com `ROLE_ADMIN`                     |

> Para recriar o banco do zero: `docker compose down -v && docker compose up -d`

## Usuarios disponiveis

| Usuario | Email                      | Senha      | Role         | Plano |
|---------|----------------------------|------------|--------------|-------|
| Demo    | `demo@investalert.com`     | `demo123`  | `ROLE_USER`  | FREE  |
| Admin   | `admin@investalert.com`    | `****` | `ROLE_ADMIN` | PRO   |

## Endpoints (v1)

Todos os endpoints sao prefixados com `/api/v1`.

### Autenticacao (publico)

| Metodo | Endpoint                | Descricao              | Status |
|--------|-------------------------|------------------------|--------|
| POST   | `/api/v1/auth/register` | Registrar novo usuario | 201    |
| POST   | `/api/v1/auth/login`    | Login (retorna JWT)    | 200    |

### Assets (autenticado)

| Metodo | Endpoint                  | Descricao                | Status   |
|--------|---------------------------|--------------------------|----------|
| GET    | `/api/v1/assets`          | Listar ativos (paginado) | 200      |
| GET    | `/api/v1/assets/{ticker}` | Buscar ativo por ticker  | 200, 404 |

### Rules (autenticado + permissao)

| Metodo | Endpoint              | Descricao       | Permissao      | Status        |
|--------|-----------------------|-----------------|----------------|---------------|
| POST   | `/api/v1/rules`       | Criar regra     | `ALERT_CREATE` | 201, 400, 403, 404 |
| GET    | `/api/v1/rules`       | Listar regras   | -              | 200           |
| PUT    | `/api/v1/rules/{id}`  | Atualizar regra | `ALERT_UPDATE` | 200, 403, 404 |
| DELETE | `/api/v1/rules/{id}`  | Remover regra   | `ALERT_DELETE` | 204, 403, 404 |

### Rule Groups (autenticado)

| Metodo | Endpoint              | Descricao             | Status   |
|--------|-----------------------|-----------------------|----------|
| POST   | `/api/v1/rule-groups` | Criar grupo de regras | 201, 400 |
| GET    | `/api/v1/rule-groups` | Listar grupos         | 200      |

### Alerts (autenticado)

| Metodo | Endpoint          | Descricao                               | Status |
|--------|-------------------|-----------------------------------------|--------|
| GET    | `/api/v1/alerts`  | Historico de alertas (paginado/filtros) | 200    |

Todos os endpoints autenticados exigem o header `Authorization: Bearer <token>`.

## Swagger UI

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```
http://localhost:8080/v3/api-docs/v1
```

## Exemplos de chamadas

Registrar usuario:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Joao", "email": "joao@example.com", "password": "senha123"}'
```

Login:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "demo@investalert.com", "password": "demo123"}'
```

Listar ativos:

```bash
curl http://localhost:8080/api/v1/assets \
  -H "Authorization: Bearer <token>"
```

Criar regra (requer `ALERT_CREATE`):

```bash
curl -X POST http://localhost:8080/api/v1/rules \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"ticker": "HGLG11", "field": "DIVIDEND_YIELD", "operator": "GREATER_THAN", "targetValue": 9.0}'
```

Criar grupo de regras:

```bash
curl -X POST http://localhost:8080/api/v1/rule-groups \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "ticker": "HGLG11",
    "name": "Meu Grupo",
    "rules": [
      {"field": "DIVIDEND_YIELD", "operator": "GREATER_THAN", "targetValue": 9.0},
      {"field": "P_VP", "operator": "LESS_THAN", "targetValue": 1.1}
    ]
  }'
```

Consultar alertas com filtro:

```bash
curl "http://localhost:8080/api/v1/alerts?ticker=HGLG11&status=SENT&page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

## Versionamento da API

A API utiliza versionamento por URI path (`/api/v{N}`). Versoes depreciadas retornam os headers `Deprecation: true` e `Sunset: <date>` em todas as respostas. A configuracao e feita via `application.yml`:

```yaml
app:
  api:
    versions:
      v1:
        deprecated: false
        # sunset-date: "2026-06-01"  # habilitar ao deprecar
```

## Variaveis de ambiente

| Variavel                        | Padrao                                         | Descricao                      |
|---------------------------------|------------------------------------------------|--------------------------------|
| `MYSQL_HOST`                    | `localhost`                                    | Host do MySQL                  |
| `MYSQL_PORT`                    | `3306`                                         | Porta do MySQL                 |
| `MYSQL_DATABASE`                | `investalert`                                  | Nome do banco                  |
| `MYSQL_USERNAME`                | `root`                                         | Usuario do banco               |
| `MYSQL_PASSWORD`                | `changeme`                                     | Senha do banco                 |
| `MYSQL_POOL_MIN_IDLE`           | `5`                                            | Minimo de conexoes ociosas     |
| `MYSQL_POOL_MAX_SIZE`           | `20`                                           | Maximo de conexoes no pool     |
| `MYSQL_POOL_IDLE_TIMEOUT`       | `30000`                                        | Timeout de conexao ociosa (ms) |
| `MYSQL_POOL_CONNECTION_TIMEOUT` | `20000`                                        | Timeout de conexao (ms)        |
| `JWT_SECRET`                    | `your-256-bit-secret-key-change-in-production` | Chave secreta para JWT         |
| `JWT_EXPIRATION_MS`             | `86400000`                                     | Expiracao do token (ms)        |
| `TZ`                            | `America/Sao_Paulo`                            | Timezone da aplicacao          |

## Testes

```bash
./mvnw test
```

A suite inclui:

- Testes unitarios para logica de dominio e use cases
- Property-based tests com jqwik (dominio, use cases, JWT, seguranca, ownership)
- Testes de integracao com H2 em memoria (seguranca, versionamento, OpenAPI)

## Build da imagem Docker

```bash
docker build -t invest-alert-api .
```

O `Dockerfile` usa multi-stage build: compila com `eclipse-temurin:25-jdk` e gera a imagem final com `eclipse-temurin:25-jre`, expondo a porta `8080`.
