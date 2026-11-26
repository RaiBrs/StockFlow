# StockFlow

StockFlow é uma aplicação web de gestão de estoque para pequenas e médias empresas, especialmente distribuidoras. O projeto busca manter saldos confiáveis por produto e depósito, impedir saídas acima do disponível e registrar quem realizou cada movimentação. Também serve como projeto de aprendizado e portfólio em Java e Spring Boot.

> **Estado atual:** o backend está em construção. Existem login JWT e rotas iniciais protegidas de produtos e depósitos. Gestão de usuários, operações de estoque, frontend e Docker Compose ainda estão pendentes.

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
- Produtos só poderão ser desativados com saldo zero em todos os depósitos. Produtos com histórico não poderão ser excluídos definitivamente.

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

- Entidades `Product`, `Warehouse` e `User`, seus repositórios e migrações Flyway `V1` a `V5`.
- Serviço interno de cadastro e consulta por ID de produtos, com normalização do SKU e recusa prévia de duplicidade. Um índice único no PostgreSQL protege a unicidade sem diferenciar maiúsculas de minúsculas.
- Serviço interno de cadastro e consulta de depósitos com normalização do código. Um índice único no PostgreSQL impede códigos equivalentes com diferenças de maiúsculas e minúsculas.
- Serviço interno de cadastro de usuários com normalização do e-mail e codificação da senha por BCrypt.
- Configuração explícita para criar o primeiro `ADMIN` quando a tabela de usuários estiver vazia; fica desligada por padrão.
- Login que confere a senha com BCrypt e emite JWT de 15 minutos. A cada acesso protegido, o backend consulta o estado e o perfil atuais do usuário; token de conta desativada ou com perfil alterado é recusado.
- DTOs de entrada e resposta para produtos e depósitos, controllers com validação e autorização por perfil, além de testes de serviço e de contrato HTTP; cadastro interno de usuários com testes unitários.

As rotas usam o prefixo `/api/v1`: `POST /auth/login`, `POST /products`, `GET /products/{id}`, `POST /warehouses` e `GET /warehouses/{id}`. O cadastro de produtos e depósitos exige `ADMIN` ou `MANAGER`; a consulta aceita também `OPERATOR`. As rotas de cadastro retornam 201, a consulta retorna 200 e erros conhecidos são traduzidos para 400, 404 ou 409. Ausência ou invalidade de autenticação resulta em 401; perfil sem permissão resulta em 403. `@Valid` ativa a validação dos DTOs na entrada HTTP. Gestão de usuários ainda não está implementada.

## Configuração local

São necessários JDK 25, Maven e PostgreSQL. A conexão aponta para `localhost:5432` e exige estas variáveis de ambiente no processo que inicia a aplicação:

| Variável | Conteúdo |
|---|---|
| `DB_NAME` | Nome do banco já criado |
| `DB_USER` | Usuário com acesso ao banco |
| `DB_PASSWORD` | Senha desse usuário |
| `JWT_SECRET_BASE64` | Chave aleatória de pelo menos 32 bytes, codificada em Base64, usada para assinar JWTs |

No IntelliJ, configure-as em **Run/Debug Configurations → Environment variables**. Para gerar uma chave local no PowerShell, execute `[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))` e coloque o resultado em `JWT_SECRET_BASE64`. Guarde a chave fora do repositório: trocá-la invalida os tokens já emitidos. O `application.yaml` não contém credenciais. O `.gitignore` exclui `.env`, configurações locais `application-local.*` e configurações compartilháveis do IntelliJ em `.run/`; confira o `git status` antes de cada commit. Criar um arquivo `.env` sozinho **não** faz o Spring Boot carregá-lo automaticamente.

Ao iniciar a aplicação com a conexão disponível, o Flyway valida as migrações `V1` a `V5`, e o Hibernate valida o esquema (`ddl-auto: validate`). Os logs da execução local mostraram a aplicação iniciando com PostgreSQL e o esquema na versão 5. Ainda não há Compose pronto.

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

Os testes selecionados de serviço, configuração inicial, contratos HTTP e segurança foram executados localmente: **40 passaram**. Eles incluem login, token expirado, perfil sem permissão e bloqueio de conta desativada. A execução exigiu apontar o Maven para um JDK 25 funcional e iniciar o Mockito como agente Java neste ambiente Windows.

A suíte completa foi executada anteriormente, mas o teste `contextLoads` falhou ao tentar abrir a conexão PostgreSQL no ambiente de testes (`Permission denied: getsockopt`). Depois disso, logs de uma execução local no IntelliJ confirmaram a conexão e a validação das cinco migrações. Os testes HTTP atuais usam MockMvc e repositório simulado; ainda não comprovam o fluxo de login completo com PostgreSQL real.

## Próximos passos

1. Verificar manualmente o login do `ADMIN` criado no PostgreSQL e o acesso com JWT às rotas protegidas.
2. Implementar gestão de usuários restrita a `ADMIN`.
3. Completar contratos de atualização e desativação de produtos e depósitos conforme as regras de estoque.
4. Criar as operações de estoque e verificar transações e concorrência com PostgreSQL.
5. Integrar o frontend e preparar Docker Compose, documentação da API e demonstração.
