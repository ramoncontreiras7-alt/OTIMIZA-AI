# Otimiza AI — Guia de Release

> **Diretriz inegociável:** a chave primária composta `(native_stop_id, platform_id)` jamais pode ser mascarada, ofuscada ou reatribuída durante o ciclo de build / R8 / release.

## 1. Build Types

| Type      | applicationId                          | minify | shrinkRes | signing             |
|-----------|----------------------------------------|--------|-----------|---------------------|
| `debug`   | `com.otimiza.delivery.debug`           | off    | off       | debug keystore      |
| `release` | `com.otimiza.delivery`                 | on (R8)| on        | debug (substituir)  |

## 2. Assinatura de release (produção)

Substitua o `signingConfig` no `app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("OTIMIZA_KEYSTORE") ?: "otimiza-release.jks")
        storePassword = System.getenv("OTIMIZA_STORE_PASSWORD")
        keyAlias = System.getenv("OTIMIZA_KEY_ALIAS")
        keyPassword = System.getenv("OTIMIZA_KEY_PASSWORD")
    }
}
buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

**Nunca** commite o `.jks` ou senhas no repositório. Use GitHub Actions Secrets.

## 3. Versionamento

- `versionCode`: incrementado a cada upload na Play Store (monotônico).
- `versionName`: segue SemVer (`major.minor.patch`). Hotfix urgente? patch.
- Tags Git disparam o workflow `deploy.yml` (release AAB).

```bash
git tag -a v1.2.3 -m "release: CPK margin-aware clustering"
git push origin v1.2.3
```

## 4. Pipeline CI/CD (`.github/workflows/`)

| Workflow    | Trigger           | Job principal                              |
|-------------|-------------------|--------------------------------------------|
| `ci.yml`    | push, PR          | JDK 21 + SDK 36 + `./gradlew test lint`    |
| `deploy.yml`| tag `v*`          | Build AAB release assinado + upload Play    |

A CI **falha** se:
- qualquer teste unitário falhar;
- lint lançar erro crítico;
- `BuildConfig.VERSION_NAME` divergir da tag Git.

## 5. Mapeamento R8 (proguard)

- Arquivo gerado: `app/build/outputs/mapping/release/mapping.txt`.
- **Upload obrigatório** para o Play Console após cada release (permite desofuscar stacktraces).
- Mantenha o `mapping.txt` arquivado por `versionCode` (compatibilidade de stacktrace).

## 6. Checklist pré-release

- [ ] `./gradlew test` passa (CI verde)
- [ ] `./gradlew :app:lintRelease` sem novos erros críticos
- [ ] `versionCode`/`versionName` incrementados
- [ ] `lint-baseline.xml` revisado (entradas expiradas devem ser removidas)
- [ ] `proguard-rules.pro` revisado para libs novas
- [ ] Sem `TODO`/`FIXME` em código de produção
- [ ] Sem credenciais hardcoded (verificar `git grep -nE "password|secret|api_key"`)
- [ ] `CHANGELOG.md` atualizado
- [ ] Tag Git criada (`vX.Y.Z`)
- [ ] Mapping arquivado para o `versionCode`

## 7. Pós-release

- [ ] Confirmar upload do AAB no Play Console
- [ ] Upload do `mapping.txt` (deobfuscation)
- [ ] Smoke test em 1 dispositivo Android real (câmera + accessibility)
- [ ] Monitorar crashlytics nas primeiras 24h

## 8. Conformidade com a Diretriz Crítica #1

Após cada release, validar em produção:

```sql
-- Não pode haver surrogate_key sobrescrevendo a chave composta
SELECT native_stop_id, platform_id, COUNT(*) AS occ
FROM delivery_stops
GROUP BY native_stop_id, platform_id
HAVING occ > 1;  -- esperado: ZERO
```

Se essa query retornar linhas, a integridade da PK composta foi violada. **Rollback imediato.**