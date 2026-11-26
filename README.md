# StockFlow

StockFlow é uma aplicação web de gestão de estoque para pequenas e médias empresas, especialmente distribuidoras. O projeto busca manter saldos confiáveis por produto e depósito, impedir saídas acima do disponível e registrar quem realizou cada movimentação. Também serve como projeto de aprendizado e portfólio em Java e Spring Boot.

> **Estado atual:** o backend está em construção. Existem login JWT, rotas iniciais de produtos, depósitos e usuários, além de saldo por produto e depósito, entradas, saídas, ajustes e histórico. Frontend e demais operações de cadastro ainda estão pendentes.

O escopo verificável está em [REQUIREMENTS.md](docs/REQUIREMENTS.md) e a sequência de entregas em [BACKLOG.md](docs/BACKLOG.md).

## Escopo planejado

O MVP inclui autenticação e autorização por perfil, gestão básica de usuários, produtos e depósitos, estoque por produto e depósito, entradas, saídas, ajustes justificados, histórico de movimentações, validação, tratamento de erros, documentação da API, testes e execução com Docker Compose.

Pedidos, reservas, transferências, alertas de estoque mínimo, abastecimento, auditoria avançada, dashboard e relatórios são previstos para uma etapa posterior.

### Regras centrais

- Cada produto tem SKU único; cada depósito tem código único.
- O estoque é controlado para cada combinação de produto e depósito e nunca pode ficar negativo.
- Saídas exigem saldo disponível suficiente. No MVP, sem reservas, o disponível equivale ao saldo físico.
- Toda alteração de estoque gera movimentação com produto, depósito, quantidade positiva, tipo, data e responsável.
- Movimentações concluídas são imutáveis; correções usam novas movimentações compensatórias.
- A entrada inicial estabelece o primeiro saldo e é restrita a `ADMIN` e `MANAGER`. Entradas e saídas comuns podem ser feitas também por `OPERATOR`.
- Ajustes manuais exigem justificativa e são restritos a `ADMIN` e `MANAGER`.
- Produtos só podem ser desativados com saldo zero em todos os depósitos. A desativação preserva saldos e histórico; produto inativo não recebe entrada inicial, entrada comum nem ajuste positivo. Após reativação, pode voltar a receber mercadorias. A exclusão definitiva, exclusiva de `ADMIN`, exige ausência de vínculos com depósitos e de histórico.
- Depósitos com mercadorias não podem ser desativados. Um depósito inativo não recebe entrada inicial, entrada comum ou ajuste positivo; após reativação, pode voltar a recebê-los. O bloqueio de desativação por operações pendentes será incorporado quando pedidos ou transferências forem modelados, pois hoje não existe estado pendente no backend.

## Tecnologias

- **Backend:** Java 25, Spring Boot 4.2.0, Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation, Maven, Flyway e PostgreSQL.
- **Segurança:** login com BCrypt, JWT validado pelo OAuth2 Resource Server e perfis `ADMIN`, `MANAGER` e `OPERATOR`.
- **Frontend planejado:** JavaScript, React e Vite.
- **Testes:** JUnit, Mockito quando necessário e Spring Security Test nas funcionalidades protegidas.

As dependências já declaradas estão no [`pom.xml`](pom.xml). A presença de uma dependência não significa que sua funcionalidade esteja concluída.

## Estrutura atual

```text
src/
├── main/
│   ├── java/com/rai/stockflow/
│   │   ├── config/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── exception/
│   │   ├── repository/
│   │   ├── security/
│   │   └── service/
│   └── resources/
│       ├── application.yaml
│       └── db/migration/
└── test/java/com/rai/stockflow/
```

O projeto ainda usa um único módulo Maven na raiz desta pasta. A separação futura entre `backend/` e `frontend/` será considerada quando o frontend for iniciado; a estrutura existente não deve ser reorganizada sem necessidade.

## O que já existe

- Entidades `Product`, `Warehouse`, `User`, `StockBalance` e `StockMovement`, seus repositórios e migrações Flyway `V1` a `V6`.
- Serviço interno de cadastro e consulta por ID de produtos, com normalização do SKU e recusa prévia de duplicidade. Um índice único no PostgreSQL protege a unicidade sem diferenciar maiúsculas de minúsculas.
- Cadastro, consulta, listagem, atualização, desativação e reativação de depósitos. O código é normalizado; um índice único no PostgreSQL impede códigos equivalentes com diferenças de maiúsculas e minúsculas.
- Serviço interno de cadastro de usuários com normalização do e-mail e codificação da senha por BCrypt.
- Configuração explícita para criar o primeiro `ADMIN` quando a tabela de usuários estiver vazia; fica desligada por padrão.
- Login que confere a senha com BCrypt e emite JWT de 15 minutos. A cada acesso protegido, o backend consulta o estado e o perfil atuais do usuário; token de conta desativada ou com perfil alterado é recusado.
- DTOs de entrada e resposta para produtos, depósitos e usuários, controllers com validação e autorização por perfil, além de testes de serviço e de contrato HTTP.
- Saldo físico por produto e depósito; cada mudança gera um movimento com responsável, tipo, quantidade, data e saldos anterior e novo. A operação inteira usa uma transação, e alterações de saldos existentes usam bloqueio pessimista no PostgreSQL.

As rotas usam o prefixo `/api/v1`: `POST /auth/login`, `POST /products`, `GET /products/{id}`, `POST /warehouses`, `GET /warehouses/{id}`, `POST /users` e `GET /users/{id}`. O cadastro de produtos e depósitos exige `ADMIN` ou `MANAGER`; a consulta aceita também `OPERATOR`. Cadastro e consulta de usuários exigem `ADMIN`. As rotas de cadastro retornam 201, a consulta retorna 200 e erros conhecidos são traduzidos para 400, 404 ou 409. Ausência ou invalidade de autenticação resulta em 401; perfil sem permissão resulta em 403. `@Valid` ativa a validação dos DTOs na entrada HTTP. Listagem, atualização e desativação de usuários ainda estão pendentes.

Depósitos também oferecem `GET /warehouses?page=0` para listagem em páginas de 20, `PUT /warehouses/{id}` para atualizar `code` e `name`, e `PATCH /warehouses/{id}/deactivate` e `/reactivate`. A listagem aceita os três perfis; atualização e mudança de estado exigem `ADMIN` ou `MANAGER`. Um depósito com saldo positivo retorna 409 ao tentar desativar. As operações de estoque e a mudança de estado bloqueiam a mesma linha de depósito para impedir entradas simultâneas à desativação.

O ciclo de vida do produto usa `PATCH /products/{id}/deactivate` e `PATCH /products/{id}/reactivate`, disponíveis a `ADMIN` e `MANAGER`, com `ProductResponse` e status 200. `DELETE /products/{id}` exige `ADMIN` e retorna 204 quando o produto nunca teve vínculo de estoque nem histórico. Produto com saldo positivo não pode ser desativado; produto com qualquer vínculo de estoque ou movimentação não pode ser excluído definitivamente. Conflitos retornam 409. As operações de estoque e a mudança de estado bloqueiam a mesma linha de produto para impedir uma entrada simultânea à desativação.

### Operações de estoque

Todas as rotas abaixo exigem JWT e usam o prefixo `/api/v1/stock`:

| Método e rota | Perfis | Efeito |
|---|---|---|
| `POST /initial-entries` | ADMIN, MANAGER | Cria o primeiro saldo do par produto e depósito |
| `POST /entries` | ADMIN, MANAGER, OPERATOR | Acrescenta unidades a saldo já iniciado |
| `POST /exits` | ADMIN, MANAGER, OPERATOR | Retira unidades até o saldo disponível |
| `POST /adjustments` | ADMIN, MANAGER | Corrige saldo com direção e justificativa |
| `GET /balances/{productId}/{warehouseId}` | Todos | Consulta saldo físico atual |
| `GET /movements/{productId}/{warehouseId}?page=0` | Todos | Consulta histórico em páginas de 20, do mais recente para o mais antigo |

As três primeiras operações recebem `productId`, `warehouseId` e `quantity` positiva. Ajustes também recebem `direction` (`IN` ou `OUT`) e `reason` obrigatória. Cada POST retorna 201 e o movimento criado, incluindo `previousBalance` e `newBalance`. Saldo ainda não iniciado retorna 404; saldo insuficiente, depósito inativo ou segunda entrada inicial retornam 409. A entrada inicial simultânea é protegida por uma restrição única no banco. Movimentos já concluídos não possuem rota de edição ou exclusão.

## Configuração local

São necessários JDK 25, Maven e PostgreSQL. A conexão aponta para `localhost:5432` e exige estas variáveis de ambiente no processo que inicia a aplicação:

| Variável | Conteúdo |
|---|---|
| `DB_NAME` | Nome do banco já criado |
| `DB_USER` | Usuário com acesso ao banco |
| `DB_PASSWORD` | Senha desse usuário |
| `JWT_SECRET_BASE64` | Chave aleatória de pelo menos 32 bytes, codificada em Base64, usada para assinar JWTs |

No IntelliJ, configure-as em **Run/Debug Configurations → Environment variables**. Para gerar uma chave local no PowerShell, execute `[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))` e coloque o resultado em `JWT_SECRET_BASE64`. Guarde a chave fora do repositório: trocá-la invalida os tokens já emitidos. O `application.yaml` não contém credenciais. O `.gitignore` exclui `.env`, configurações locais `application-local.*` e configurações compartilháveis do IntelliJ em `.run/`; confira o `git status` antes de cada commit. Criar um arquivo `.env` sozinho **não** faz o Spring Boot carregá-lo automaticamente.

Ao iniciar a aplicação com a conexão disponível, o Flyway aplica as migrações `V1` a `V6`, e o Hibernate valida o esquema (`ddl-auto: validate`). A versão 6 foi aplicada em um PostgreSQL de teste isolado.

### Executar com Docker Compose

O Compose inicia um **banco separado** do PostgreSQL local. Ele publica o banco em `localhost:5433`, para não ocupar a porta `5432` já usada no computador, e a API em `localhost:8080`. Defina `DB_NAME`, `DB_USER`, `DB_PASSWORD` e `JWT_SECRET_BASE64` no terminal que executará o Compose. As variáveis salvas apenas na configuração do IntelliJ não chegam automaticamente ao terminal. O arquivo `.env` local também pode fornecer essas variáveis ao Compose e está ignorado pelo Git.

```powershell
mvn -DskipTests package
docker compose up --build -d
docker compose ps
```

O `Dockerfile` usa o JAR gerado por Maven. Na primeira inicialização, o Flyway cria o esquema no volume `stockflow_pgdata`. Para usar o primeiro administrador nesse banco novo, configure as variáveis de setup para uma única execução; retire a senha e desative o modo depois. `docker compose down` para os contêineres e preserva o volume. Não use `down -v` se quiser manter os dados.

### Criar o primeiro administrador

Na configuração de execução do IntelliJ, mantenha as variáveis do banco e adicione as seguintes **somente para a execução inicial**:

| Variável | Conteúdo |
|---|---|
| `STOCKFLOW_SETUP_ADMIN_ENABLED` | `true` |
| `STOCKFLOW_SETUP_ADMIN_EMAIL` | E-mail escolhido para o primeiro administrador |
| `STOCKFLOW_SETUP_ADMIN_PASSWORD` | Senha escolhida, definida apenas no ambiente local |

Inicie `StockFlowApplication`. O modo inicial recusa credenciais em branco ou uma tabela `app_users` que já contenha usuários. Em caso de sucesso, o serviço de cadastro normaliza o e-mail, aplica BCrypt à senha e salva o perfil `ADMIN`; o log informa a criação sem exibir a senha. **Antes da próxima execução, remova ou desative `STOCKFLOW_SETUP_ADMIN_ENABLED` e remova a senha da configuração inicial.** O modo fica desligado por padrão. Essa opção existe para resolver o primeiro acesso: a gestão normal de usuários será exclusiva de `ADMIN`, mas inicialmente não há nenhum. Depois de criada a conta, manter a opção ou sua senha na configuração não é necessário.

### Fazer login

Envie `POST /api/v1/auth/login` com corpo JSON:

```json
{"email":"admin@exemplo.com","password":"<senha definida localmente>"}
```

Uma resposta válida contém `accessToken`, `tokenType: "Bearer"` e `expiresIn: 900`. Nas rotas protegidas, envie `Authorization: Bearer <accessToken>`. Não registre senhas ou tokens em logs nem os inclua em commits. A senha é verificada somente no login; o token assinado é validado nas requisições seguintes, junto do estado atual do usuário. A criação inicial do `ADMIN` não precisa ser ativada novamente para fazer login.

## Testes e estado da verificação

Os testes de serviço e HTTP cobrem validação, autorização, ciclo de vida de produtos e depósitos, e operações de estoque. A suíte completa foi executada com um PostgreSQL isolado: migração V6, unicidade de SKU, concorrência entre saídas, corridas entre entrada e desativação de produto ou depósito, e rollback quando um movimento viola uma restrição SQL foram verificados. `docker compose config --quiet` também validou a configuração.

`ProductSkuPostgresTest` usa PostgreSQL real e só executa a verificação quando `DB_NAME`, `DB_USER` e `DB_PASSWORD` estão disponíveis no processo de testes. Por padrão, conecta em `localhost:5432`; para o banco do Compose, defina `STOCKFLOW_TEST_DB_URL=jdbc:postgresql://localhost:5433/<DB_NAME>`. O teste faz rollback e não deixa produto cadastrado. Uma execução em que ele seja ignorado não comprova a restrição no banco.

`StockConcurrencyPostgresTest` é executado quando `STOCKFLOW_RUN_DB_TESTS=true` e as variáveis de banco apontam para um PostgreSQL de teste. Use `SPRING_DATASOURCE_URL` para indicar a porta do banco quando não for `5432`. Os testes HTTP de estoque usam MockMvc e serviço simulado; o fluxo completo de login com o ADMIN do seu banco local ainda precisa de verificação manual.

## Próximos passos

1. Verificar manualmente o login do `ADMIN` criado no PostgreSQL e o acesso com JWT às rotas protegidas.
2. Completar gestão de usuários restrita a `ADMIN`.
3. Completar listagem e atualização de produtos; adicionar a verificação de operações pendentes no depósito quando esse tipo de operação existir.
4. Integrar login e operações de estoque em um teste HTTP ponta a ponta com PostgreSQL.
5. Integrar o frontend e validar Docker Compose, documentação da API e demonstração.
