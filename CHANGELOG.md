# Changelog

Todas as mudanças relevantes do **Otimiza AI** são listadas aqui.
Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

## [Unreleased]

### Adicionado
- `PlatformPatterns` — validação de IDs nativos via regex (IFOOD, MLB, LALA) + deduplicação temporal (30s).
- `VrpEngineClientImpl` — retry exponencial (3 tentativas) com fallback offline-first preservando IDs nativos.
- `MapLibreMapContainer` — renderização Mapbox/MapLibre SDK 10.x com cluster por cor e ciclo de vida Compose.
- `proguard-rules.pro` — regras R8 com preservação de value classes, Room (PK composta), Retrofit, Hilt, ML Kit, CameraX, Mapbox.
- `lint-baseline.xml` — baseline de exceções documentadas.
- `RELEASE.md` — checklist operacional de release.
- `init-github.ps1` — script de bootstrap do repositório GitHub (`otimiza-ai`, SSH ou PAT).
- `PUBLISH.md` — guia passo a passo para publicação no GitHub como `otimiza-ai`.
- CI artifact: APK debug (`otimiza-ai-debug-apk`) anexado a cada push.
- Release artifact: AAB assinado (`otimiza-ai-release-aab`) + mapping R8 (`otimiza-ai-mapping`) em tags `v*.*.*`.
- `ARCHITECTURE.md` — diagramas Mermaid (camadas, fluxo PK composta, pipeline CI/CD).

### Modificado
- `app/build.gradle.kts` — habilita R8 (`isMinifyEnabled=true`), `isShrinkResources=true`, lint baseline, build types `debug`/`release` separados.

### Segurança
- Assinatura release via variáveis de ambiente (sem credenciais no repo).