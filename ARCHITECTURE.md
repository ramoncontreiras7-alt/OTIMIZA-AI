# Otimiza AI — Arquitetura

> Documento visual da arquitetura: **camadas Clean**, **fluxo de imutabilidade do ID nativo**, **pipeline CI/CD**.

---

## 1. Visão geral — Camadas

```mermaid
flowchart TB
    subgraph UI["🎨 UI Layer (Jetpack Compose)"]
        SCAN[LabelScannerScreen<br/>+ LabelOcrAnalyzer]
        MAP[UnifiedMapScreen<br/>+ MapLibreMapContainer]
    end

    subgraph SERVICE["⚙️ Android Service"]
        A11Y[DeliveryAccessibilityService]
    end

    subgraph DOMAIN["🧠 Domain Layer (UseCases + Modelos)"]
        ROUTE[UnifiedRoutingUseCase]
        CPK[CalculateRouteCpKUseCase]
        PARSE[DocumentParserUseCase]
        PAT[PlatformPatterns]
        MOD[DeliveryStop · Platform<br/>NativeStopId · PlatformId]
    end

    subgraph DATA["💾 Data Layer"]
        REPO[DeliveryRepositoryImpl]
        DAO[DeliveryStopDao]
        ENT[DeliveryStopEntity<br/>PK composta: native_stop_id + platform_id]
        DB[(Room Database)]
        MAPF[DeliveryStopMapper]
        REMOTE[VrpEngineClient<br/>+ VrpApiService]
    end

    subgraph DI["💉 Hilt DI"]
        APP[AppModule]
        RM[RepositoryModule]
        ROM[RoomModule]
        NET[RetrofitModule]
    end

    SCAN --> PAT
    SCAN --> REPO
    A11Y --> REPO
    MAP --> REPO
    MAP --> ROUTE

    REPO --> ROUTE
    REPO --> DAO
    REPO --> REMOTE

    ROUTE --> DAO
    ROUTE --> REMOTE
    CPK --> MOD
    PARSE --> MOD

    DAO --> ENT
    ENT --> DB
    DAO --> MAPF
    MAPF --> MOD

    APP --> REPO
    RM --> REPO
    ROM --> DAO
    ROM --> DB
    NET --> REMOTE
```

---

## 2. Fluxo de captura (Edge AI → Banco Local)

```mermaid
sequenceDiagram
    autonumber
    actor Motorista
    participant Cam as 📷 CameraX
    participant OCR as 🤖 LabelOcrAnalyzer
    participant PAT as 🔍 PlatformPatterns
    participant REPO as 💾 DeliveryRepositoryImpl
    participant DAO as 🗄️ DeliveryStopDao
    participant DB as (Room)

    Motorista->>Cam: Aponta para etiqueta
    Cam->>OCR: ImageProxy (frame)
    OCR->>PAT: identifyPlatformAndId(texto)
    PAT-->>OCR: (Platform, nativeStopId) | null
    alt ID válido
        OCR->>PAT: isDuplicate(nativeStopId)
        alt Não duplicado
            OCR->>REPO: saveStop(DeliveryStop)
            REPO->>DAO: upsert(entity)
            DAO->>DB: INSERT OR IGNORE
            Note over DB: PK composta<br/>(native_stop_id, platform_id)<br/>preservada verbatim
        else Duplicado (≤30s)
            OCR-->>OCR: descarta frame
        end
    else Texto inválido
        OCR-->>OCR: descarta frame
    end
```

---

## 3. Otimização VRP concorrente multi-plataforma

```mermaid
sequenceDiagram
    autonumber
    participant UI as UnifiedMapScreen
    participant UC as UnifiedRoutingUseCase
    participant REPO as DeliveryRepositoryImpl
    participant DAO as DeliveryStopDao
    participant DB as (Room)
    participant VRP as VrpEngineClient<br/>(OSRM/GraphHopper)

    UI->>UC: invoke(sessionId)
    UC->>REPO: findStopsBySession(sessionId)
    REPO->>DAO: findBySession(sessionId)
    DAO->>DB: SELECT * FROM delivery_stops<br/>WHERE route_id = ?
    DB-->>DAO: List<DeliveryStopEntity>
    DAO-->>REPO: entidades
    REPO-->>UC: List<DeliveryStop> (domínio)

    UC->>VRP: optimizeRoute(stops)
    Note over VRP: Retry/backoff<br/>3 tentativas
    VRP-->>UC: List<DeliveryStop> reordenado
    Note over UC: IDs nativos preservados<br/>na reordenação

    UC-->>UI: Result<List<DeliveryStop>>
    UI->>UI: renderiza marcadores<br/>com cor/ícone por plataforma
```

---

## 4. Imutabilidade do ID nativo (Diretriz Crítica #1)

```mermaid
flowchart LR
    subgraph FONTE["Origem (nunca modificada)"]
        A[iFood<br/>IFOOD-123456]
        B[Mercado Livre<br/>MLB123456789]
        C[Lalamove<br/>LALA-999888]
    end

    subgraph DOMAIN["Domain (value class, val)"]
        N[NativeStopId]
        P[PlatformId]
    end

    subgraph ENTITY["Room Entity (PK composta)"]
        E["DeliveryStopEntity<br/>primaryKeys = ['native_stop_id', 'platform_id']<br/>surrogateKey? = null (opcional)"]
    end

    subgraph MAPA["Mapa Unificado (verbatim)"]
        M["buildMarkerTag(stop)<br/>'native:' + id.value + '|platform:' + externalRef.value"]
    end

    A --> N
    B --> N
    C --> N
    N --> E
    P --> E
    E --> M

    classDef critical fill:#fee,stroke:#c00,stroke-width:3px
    class E critical
    class M critical
```

**Garantias:**
- `NativeStopId` e `PlatformId` são `@JvmInline value class` → sem boxing, imutáveis.
- `DeliveryStop` é `data class` com todos os campos `val`.
- Room exige `primaryKeys = ["native_stop_id", "platform_id"]` → impossível inserir sem a chave composta.
- `Marker.snippet` recebe `buildMarkerTag(stop)` verbatim — UI nunca transforma o ID.

---

## 5. Pipeline CI/CD

```mermaid
flowchart LR
    DEV([👨‍💻 Dev]) -->|git push| CI[".github/workflows/ci.yml<br/>JDK 21 + SDK 36"]
    CI --> TEST["./gradlew test<br/>(JUnit 5 + MockK)"]
    CI --> LINT["./gradlew :app:lintRelease"]
    TEST --> CHECK{pass?}
    LINT --> CHECK
    CHECK -->|sim| GREEN([✅ Build verde])
    CHECK -->|não| RED([❌ PR bloqueado])

    DEV -->|git tag v*| DEPLOY[".github/workflows/deploy.yml"]
    DEPLOY --> BUILD["./gradlew bundleRelease<br/>(R8 + ProGuard)"]
    BUILD --> SIGN[Assinatura via<br/>OTIMIZA_KEYSTORE env]
    SIGN --> UPLOAD[Upload AAB → Play Console]
    UPLOAD --> MAP[Upload mapping.txt]
    MAP --> DONE([🚀 Release publicado])
```

---

## 6. Mapa de dependências (módulos)

```mermaid
graph TD
    APP[":app<br/>(com.otimiza.delivery)"]

    APP --> CORE[androidx.core]
    APP --> CMP[compose-bom + material3]
    APP --> LC[lifecycle-runtime-ktx<br/>+ viewmodel-compose]
    APP --> COR[kotlinx.coroutines]
    APP --> HILT[dagger-hilt]
    APP --> ROOM[androidx.room<br/>+ room-ktx]
    APP --> CAM[camera-camera2<br/>+ lifecycle + view]
    APP --> MLK[mlkit:text-recognition]
    APP --> NET[retrofit + okhttp<br/>+ logging]
    APP --> MAP[mapbox-android-sdk<br/>10.15.0]

    APP -.->|testImpl| JUNIT[junit-jupiter]
    APP -.->|testImpl| MOCKK[mockk 1.14.11]
    APP -.->|testImpl| COR_TEST[kotlinx-coroutines-test]
```

---

## 7. Estrutura de pastas (resumo)

```
otimiza-delivery/
├── app/
│   ├── build.gradle.kts          ← buildTypes, R8, lint
│   ├── proguard-rules.pro
│   ├── lint-baseline.xml
│   ├── schemas/                  ← schema Room versionado
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/otimiza/delivery/
│       │   │   ├── MainActivity.kt
│       │   │   ├── OtimizaAIApplication.kt
│       │   │   ├── data/        (local · remote · repository)
│       │   │   ├── di/          (Hilt modules)
│       │   │   ├── domain/      (model · repository · usecase · util)
│       │   │   ├── service/     (AccessibilityService)
│       │   │   ├── ui/          (scanner · map)
│       │   │   └── util/        (GlobalExceptionHandler)
│       │   └── res/             (xml · values)
│       └── test/java/com/otimiza/delivery/
│           ├── data/
│           └── domain/
├── .github/workflows/            (ci · deploy)
├── .kilo/                        (agent · command)
├── AGENTS.md
├── CHANGELOG.md
├── RELEASE.md
├── RELEASE_PIPELINE.md
└── ARCHITECTURE.md               ← este arquivo
```

---

## 8. Como navegar

| Quer ver... | Abra... |
|---|---|
| PK composta | `data/local/entity/DeliveryStopEntity.kt` |
| Pipeline VRP | `domain/usecase/UnifiedRoutingUseCase.kt` + `data/remote/VrpEngineClient.kt` |
| OCR + dedup | `domain/util/PlatformPatterns.kt` + `ui/scanner/LabelOcrAnalyzer.kt` |
| Mapa | `ui/map/MapLibreMapContainer.kt` |
| R8 rules | `app/proguard-rules.pro` |
| Release | `RELEASE.md` |