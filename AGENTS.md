# AGENTS.md — Otimiza AI (Android Delivery Router)

> Guia de onboarding para agentes Kilo operando neste repositório.

## Visão Geral
`Otimiza AI` é um app Android Kotlin (Jetpack Compose) que **consolida rotas de entrega de iFood, Mercado Livre e Lalamove em um único motor VRP** (OSRM/GraphHopper), renderizando tudo num **mapa unificado** (MapLibre) com diferenciação por cores. ID nativo NUNCA é mascarado.

## Arquitetura (Clean Layered)
```
domain/        → model (DeliveryStop, NativeStopId, PlatformId, Platform, RouteFinancialMetrics)
               → usecase (UnifiedRoutingUseCase, CalculateRouteCpKUseCase, DocumentParserUseCase)
               → repository (interface DeliveryRepository)
data/          → local  (Room: DeliveryStopEntity[PK composta], DAO, AppDatabase, mapper, PlatformConverter)
               → remote (VrpApiService + VrpEngineClientImpl, OkHttp interceptors)
               → repository (DeliveryRepositoryImpl)
ui/            → map (UnifiedMapScreen, UnifiedMapViewModel, PlatformMarkerStyle), scanner (CameraX+ML Kit)
service/       → DeliveryAccessibilityService (screen reading)
di/            → RetrofitModule, RoomModule, RepositoryModule (Hilt @SingletonComponent)
util/          → GlobalExceptionHandler
```

## Regra de Ouro — Imutabilidade do ID nativo (Diretriz Crítica #1)
- Chave primária da table `delivery_stops`: **composta por `native_stop_id` + `platform_id`** (nunca autoincremento).
- O `native_stop_id` é `val` e passa `OCR → Repository → Room → VRP-engine → Mapa` sem **hash / trim / reatribuição**.
- O DAO: `UPDATE` só altera `sequence` (nunca as colunas de ID). Verificado por `DeliveryStopMapperTest`, `VrpEngineClientImplTest`, `UnifiedRoutingUseCaseTest`.

## Stack (2026)
Kotlin 2.2 · AGP 8.9 · Hilt 2.52 · Room 2.7 · CameraX 1.4 · ML Kit 16 · MapLibre 10.15 · Retrofit 2.11 · OkHttp 4.13 · MockK 1.14.11 · JUnit 5.11.

## Testes
- Framework: JUnit 5 + MockK 1.14.11 + kotlinx-coroutines-test (`runTest`).
- Padrão do agente **`qa-tester`** (.kilo/agent/qa-tester.md): `@ExtendWith(MockKExtension)`, `@RelaxedMockK`, `@InjectMockKs`, `confirmVerified`, `@CheckUnnecessaryStub`.
- `./gradlew test` (useJUnitPlatform ativo em `app/build.gradle.kts`).
- Cobertura: `CalculateRouteCpKUseCaseTest`, `UnifiedRoutingUseCaseTest`, `DeliveryRepositoryImplTest`, `VrpEngineClientImplTest`, `DeliveryStopMapperTest`.

## CI/CD
- `/run-tests` → `./gradlew test ` (roteado ao `qa-tester`).
- `/deploy` → publica AAB via `.github/workflows/deploy.yml` (trigger em tags `v*.*.*`).
- GitHub Actions (`.github/workflows/ci.yml`): JDK 21 + Android SDK 36, matrix de testes.

## Comandos úteis
| Comando | Ação |
|---|---|
| `/run-tests` | Suíte unitária completa |
| `/run-tests "<NameTest>"` | Teste específico |
| `/deploy` | Build release + publicação |
