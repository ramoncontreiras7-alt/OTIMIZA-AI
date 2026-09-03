---
description: Roda a suíte de testes unitários (JUnit 5 + MockK 1.14.11) via Gradle
agent: qa-tester
---
Roda a suíte de testes unitários do módulo `app`.

## Uso
`/run-tests "<TestClassPattern>"` — executa apenas os testes que casam com o padrão (ex: `UnifiedRoutingUseCaseTest*`).
`/run-tests` — executa a suíte completa.

## Comando

\`\`\`bash
./gradlew test --no-daemon --console=plain ${ $1 ? "--tests '$1'" : "" }
\`\`\`

Argumentos extras via `$ARGUMENTS`. Referencia arquivos com `@file`.

## Saída
- Relatório HTML: `app/build/reports/tests/test/index.html`
- Resultados JUnit XML: `app/build/test-results/test/`

> Este comando é roteado ao agente `qa-tester`. O agente valida contra o padrão MockK 2026 (`@ExtendWith(MockKExtension)`, `runTest`, `ConfirmVerification`) e corrige falhas antes de finalizar.
