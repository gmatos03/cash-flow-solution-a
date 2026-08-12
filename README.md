# Cash Flow Control — Solução A (Event Sourcing + CQRS)

*[Read this in English](README.ENG.md)*

Código-fonte complementar aos Apêndices D e F do *Relatório de Arquitetura de Fluxo de
Caixa* (`Architecture1_Equivalent_Solutions.docx`). Quatro microsserviços independentes
em Spring Boot 3.2 / Java 17 implementam o lado de escrita, o lado de leitura, a projeção
e o relatório diário da Solução A, configurados para rodar inteiramente em **localhost**
contra Kafka, PostgreSQL e Redis.

Na AWS (Apêndice D), esses serviços rodam em ECS/EKS atrás de um ALB, com Amazon MSK,
Aurora PostgreSQL e ElastiCache for Redis. Localmente, o `docker-compose.yml` sobe essas
mesmas três peças de infraestrutura para que os serviços se comportem de forma idêntica
em um notebook.

| Serviço                  | Porta | Função                                                              | API pública |
|---------------------------|------|----------------------------------------------------------------------|------------|
| `command-service`        | 8081 | Valida lançamentos, anexa eventos, publica no Kafka                  | Sim (`/commands`) |
| `query-service`          | 8082 | Serve saldos/extratos/relatórios, cache-aside no Redis                | Sim (`/queries`) |
| `event-handler-service`  | 8083 | Consome o Kafka, projeta o read model, invalida o cache               | Não (interna + `/actuator`) |
| `reporting-service`      | 8084 | Constrói o log diário de fluxo de caixa (agendado + sob demanda)      | Sim (`/reports/run`) |

Cada serviço é um módulo Maven totalmente independente — não há JAR compartilhado entre
eles, o que espelha como seriam construídos e implantados separadamente nos clusters
ECS/EKS do Apêndice D.

## 1. Pré-requisitos

- Java 17+ (`java -version`)
- Maven 3.9+ (`mvn -version`)
- Docker + Docker Compose

> Este código foi escrito e revisado em um ambiente sandboxed sem acesso a Maven/Docker,
> portanto não foi compilado ali. Tudo segue as APIs padrão de Spring Boot 3.2 / Spring
> Kafka 3.1 / jjwt 0.12, mas execute `mvn clean verify` depois de baixar o repositório e
> registre para si mesmo qualquer coisa que não compile.

## 2. Suba a infraestrutura

```bash
docker compose up -d
docker compose ps   # aguarde postgres, redis e kafka reportarem "healthy"
```

Isso cria um banco Postgres `cashflow` (usuário/senha `cashflow`/`cashflow`), um broker
Kafka de nó único em `localhost:9092`, e Redis em `localhost:6379`.

## 3. Compile e execute os serviços

A partir da raiz do repositório, `mvn clean install` compila os quatro módulos. Cada um
também roda de forma independente:

```bash
cd command-service   && mvn spring-boot:run    # http://localhost:8081
cd query-service     && mvn spring-boot:run    # http://localhost:8082
cd event-handler-service && mvn spring-boot:run # http://localhost:8083
cd reporting-service  && mvn spring-boot:run    # http://localhost:8084
```

O primeiro serviço iniciado contra um banco novo aplica a migração Flyway (`V1__init.sql`)
que cria `event_store`, `accounts`, `ledger_entries` e `daily_cash_flow_log`, e semeia duas
contas de demonstração (`acc-10293847`, `acc-55510023`). Os outros três serviços apontam
para a mesma tabela de histórico de schema, então suas próprias cópias da migração são
no-ops seguros (veja o comentário no topo de cada `V1__init.sql`).

Cada serviço também cria automaticamente os tópicos Kafka de que precisa
(`ledger.credit-debit.events`, `ledger.credit-debit.events.dlq`) na inicialização, via um
bean `NewTopic`.

## 4. Autentique-se

Todo endpoint público, exceto `/auth/token` e `/actuator/health`, exige um JWT. Isso é um
substituto **apenas para desenvolvimento local** do user pool do Amazon Cognito descrito
no Apêndice D/F — veja o comentário da classe `JwtService` em cada serviço. Credenciais
de demonstração: `demo` / `demo123`.

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['accessToken'])")
```

(`command-service` e `query-service` emitem, cada um, seu próprio token contra sua
própria cópia do repositório de usuários de demonstração — qualquer um dos dois funciona
com qualquer um dos serviços, já que ambos são assinados com o mesmo segredo local
compartilhado em `application.yml`.)

## 5. Exercite o caminho de escrita

```bash
curl -s -X POST http://localhost:8081/commands/entries \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "acc-10293847",
    "amount": 1250.00,
    "currency": "USD",
    "type": "CREDIT",
    "channel": "WEB",
    "description": "Wire transfer received",
    "idempotencyKey": "'"$(uuidgen)"'"
  }' | python3 -m json.tool
```

Você deve receber `202 Accepted` com um `entryId`. Observe os logs do
`event-handler-service` — em milissegundos ele deve registrar que projetou o lançamento
e invalidou o cache de saldo. Reenviar exatamente o mesmo corpo de requisição (mesma
`idempotencyKey`) retorna `409 Conflict` com o `entryId` original, em vez de criar uma
duplicata.

## 6. Exercite o caminho de leitura

```bash
curl -s http://localhost:8082/queries/accounts/acc-10293847/balance \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

curl -s "http://localhost:8082/queries/accounts/acc-10293847/statement?from=2026-08-01&to=2026-08-10&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

A primeira leitura de saldo após um novo lançamento reporta `"source": "db"`; toda leitura
nos 300 segundos seguintes (ou até a próxima projeção) reporta `"source": "cache"`.

## 7. Construa o log diário de fluxo de caixa

```bash
curl -s -X POST "http://localhost:8084/reports/run?date=$(date -u +%F)" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool

curl -s "http://localhost:8082/queries/accounts/acc-10293847/daily-log/$(date -u +%F)" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

O `reporting-service` também executa isso automaticamente todo dia às 23:00 UTC
(`app.reporting.cron` em seu `application.yml`). De qualquer forma, procure em
`reporting-service/reports/{accountId}/{date}/` pelo `report.csv` e `report.pdf`
renderizados — isso substitui o bucket S3 de relatórios usado na implantação AWS.

## 8. Execute os testes

```bash
mvn test          # a partir da raiz do repositório, executa as suítes JUnit 5 / Mockito dos quatro módulos
```

Cada serviço tem testes unitários para sua camada de serviço (mocks Mockito para
repositórios, Kafka e Redis) e pelo menos um teste MockMvc para seu(s) controller(s),
cobrindo o caminho feliz mais os casos de erro de validação/conflito/não encontrado
documentados no Apêndice F.

## 9. Documentação da API (Swagger / OpenAPI)

`command-service`, `query-service` e `reporting-service` publicam, cada um, documentação
de API ao vivo e navegável via springdoc-openapi. `event-handler-service` não tem API
pública (é apenas consumidor Kafka), por isso está excluído.

| Serviço             | Swagger UI                                    | Spec bruta                          |
|----------------------|------------------------------------------------|--------------------------------------|
| `command-service`   | http://localhost:8081/swagger-ui.html         | http://localhost:8081/v3/api-docs |
| `query-service`     | http://localhost:8082/swagger-ui.html         | http://localhost:8082/v3/api-docs |
| `reporting-service` | http://localhost:8084/swagger-ui.html         | http://localhost:8084/v3/api-docs |

`/swagger-ui/**` e `/v3/api-docs/**` são permitidos sem token no `SecurityConfig`, mas os
endpoints que eles descrevem não são — clique em "Authorize" na UI, cole um bearer token
obtido em `POST /auth/token`, e você pode exercitar a API real pelo navegador.

Cada serviço também traz uma cópia versionada e escrita à mão de sua spec em
`<service>/docs/openapi.yaml`, útil para gerar clientes ou importar no Postman sem uma
instância em execução.

## 10. Layout do projeto

```
cash-flow-solution-a/
  docker-compose.yml          # Postgres, Redis, Kafka (KRaft, nó único)
  pom.xml                     # pom agregador (reactor)
  command-service/
    docs/openapi.yaml         # spec estática OpenAPI 3.0
    src/main/java/.../{config,security,web,domain,service,repository,kafka}
    src/main/resources/{application.yml, db/migration/V1__init.sql}
    src/test/java/...
  query-service/      (mesma estrutura, lado de leitura, + docs/openapi.yaml)
  event-handler-service/ (mesma estrutura, consumidor Kafka, sem controller de segurança/web)
  reporting-service/  (mesma estrutura, + agendador + renderização PDF/CSV, + docs/openapi.yaml)
```

## 11. O que foi simplificado para uso local (e por quê)

- **Autenticação**: um emissor JWT local HS256 (`/auth/token`) substitui o Amazon
  Cognito. Trocar por uma configuração real de OAuth2 resource-server contra o endpoint
  JWKS do Cognito é um substituto direto para `JwtService`/`JwtAuthenticationFilter` —
  nada mais muda.
- **Entrega de relatórios**: os arquivos são gravados em `./reports/...` em disco e uma
  linha de log substitui a notificação SNS/SES do Apêndice F.7, em vez de S3 + e-mail.
  `ReportRenderer.writeToLocalStorage` é o único ponto que mudaria.
- **Migrações de schema**: cada serviço traz uma cópia idêntica de `V1__init.sql`, de
  forma que qualquer um deles possa inicializar um banco novo; o Apêndice F.6 descreve
  como executar isso como uma etapa de deploy única e dedicada, em vez disso, em um
  ambiente real.
- **Dimensionamento dos tópicos Kafka**: 3 partições localmente, em vez das 12
  documentadas no Apêndice F.5, já que uma demonstração em escala de notebook não precisa
  de tanto paralelismo.
