# Otimiza AI — Release Pipeline

> Camada estática do ciclo de produção: **R8 / Lint / Versionamento / Mapping / Assinatura**.

## Artefatos entregues

| Arquivo                      | Função                                                                 |
|------------------------------|------------------------------------------------------------------------|
| `app/proguard-rules.pro`     | Regras R8 com preservação da PK composta e libs nativas.               |
| `app/lint-baseline.xml`      | Baseline de exceções documentadas.                                     |
| `app/build.gradle.kts`       | `buildTypes` (debug/release), lint baseline, R8 habilitado.            |
| `RELEASE.md`                 | Checklist operacional de release + query de validação da PK composta.   |
| `CHANGELOG.md`               | Histórico de mudanças.                                                 |

## Conformidade com a Diretriz Crítica #1

O **R8** foi configurado para:
- preservar value classes (`NativeStopId`, `PlatformId`) — sem ofuscação da identidade;
- manter nomes de colunas `native_stop_id` / `platform_id` da `DeliveryStopEntity`;
- reter DTOs Retrofit (round-trip VRP ↔ backend).

A query de validação no `RELEASE.md §8` é o **gate final**: zero ocorrências de `(native_stop_id, platform_id)` duplicado garante que a chave composta sobreviveu ao pipeline.

## Pipeline CI/CD

```
push / PR            ──►  ci.yml     ──►  ./gradlew test lint  (JDK 21, SDK 36)
git tag v*.*.*       ──►  deploy.yml ──►  ./gradlew bundleRelease + upload Play Console
```

Próximos passos pós-CI:
1. Primeiro build AAB local para validar mapping.
2. Upload do `mapping.txt` ao Play Console.
3. Ativação de Crashlytics com auto-symbolication.