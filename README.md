# StockFlow

StockFlow é uma aplicação web de gestão de estoque para pequenas e médias empresas, especialmente distribuidoras. O projeto busca manter saldos confiáveis por produto e depósito, impedir saídas acima do disponível e registrar quem realizou cada movimentação. Também serve como projeto de aprendizado e portfólio em Java e Spring Boot.

> **Estado atual:** o backend está em construção. Ainda não há API de produtos ou depósitos, autenticação, operações de estoque, frontend ou ambiente Docker Compose funcional.

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
- **Segurança planejada:** JWT, OAuth2 Resource Server, BCrypt e perfis `ADMIN`, `MANAGER` e `OPERATOR`.
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

- Entidades `Product`, `Warehouse` e `User`, seus repositórios e migrações Flyway `V1` a `V4`.
- Serviço interno de cadastro de produtos com normalização do SKU e recusa prévia de duplicidade. Um índice único no PostgreSQL protege a unicidade sem diferenciar maiúsculas de minúsculas.
- Serviço interno de cadastro de usuários com normalização do e-mail e codificação da senha por BCrypt.
- DTO inicial de entrada de produto e testes unitários de cadastro de produto e usuário.

Esses serviços ainda não estão expostos por controllers. A validação anotada no DTO de produto só será acionada na fronteira HTTP quando houver um controller com `@Valid`.

## Execução e testes

São necessários JDK 25 e Maven para compilar o backend. A aplicação ainda não possui configuração de conexão com PostgreSQL nem um Compose pronto; portanto, **não há um procedimento completo de execução da aplicação** nesta fase.

Os testes unitários de produto e usuário foram executados localmente: **6 passaram**. A execução exigiu apontar o Maven para um JDK 25 funcional e iniciar o Mockito como agente Java neste ambiente Windows. A suíte completa, as migrações em PostgreSQL e os fluxos HTTP ainda não foram validados.

## Próximos passos

1. Fechar os contratos iniciais da API para produtos, depósitos e usuários.
2. Implementar autenticação e autorização antes de disponibilizar operações protegidas.
3. Criar as operações de estoque e verificar transações e concorrência com PostgreSQL.
4. Integrar o frontend e preparar Docker Compose, documentação da API e demonstração.
