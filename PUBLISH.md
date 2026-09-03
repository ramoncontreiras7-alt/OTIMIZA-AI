# Publicação do Otimiza AI no GitHub

> Nome do repositório: **`otimiza-ai`** (não `otimiza-delivery`).
> O nome do pacote Android segue sendo `com.otimiza.delivery` (FQCN), apenas o **repo** muda.

## 1. Criar o repositório VAZIO

Acesse **https://github.com/new** e preencha **exatamente**:

| Campo | Valor |
|---|---|
| **Repository name** | `otimiza-ai` |
| **Description** | `Roteirização logística multi-plataforma Android — iFood, Mercado Livre, Lalamove` (opcional) |
| **Visibilidade** | `Public` (recomendado) ou `Private` |
| **Initialize with...** | ❌ **NÃO marque** nada (README/.gitignore/license) — já temos tudo no worktree |

Clique em **Create repository**.

## 2. Configurar suas credenciais no script

Abra `init-github.ps1` (na raiz do projeto) e edite **apenas** o bloco de configuração:

```powershell
$GithubUser = "seu-usuario-github"   # ← OBRIGATÓRIO: seu user do GitHub
$RepoName   = "otimiza-ai"           # já está correto
$Branch     = "master"               # ou "main" se preferir
$UseSsh     = $true                  # true = SSH; false = PAT
```

## 3. Executar

```powershell
cd C:\.kilo\worktrees\otimiza-delivery
powershell -ExecutionPolicy Bypass -File .\init-github.ps1
```

O script automaticamente:
- Configura `user.name` / `user.email` globais (se faltarem).
- Gera chave SSH `~/.ssh/id_ed25519` e exibe a pública.
- Aguarda você colar a chave em https://github.com/settings/keys.
- Testa `ssh -T git@github.com`.
- Faz `git push -u origin master`.

## 4. Verificar publicação

URL final:
```
https://github.com/SEU-USUARIO/otimiza-ai
```

Você deve ver:
- 2 commits (`feat: scaffold…` e `release: pipeline estatico…`)
- 30+ classes Kotlin
- Workflows `.github/workflows/ci.yml` e `deploy.yml`
- `init-github.ps1`, `ARCHITECTURE.md`, `RELEASE.md`, `CHANGELOG.md`

## 5. Baixar o APK debug

Acesse https://github.com/SEU-USUARIO/otimiza-ai/actions
- Clique no primeiro workflow **CI - Otimiza AI** (verde)
- Role até **Artifacts** → `otimiza-ai-debug-apk` → Download
- Instale no celular: `adb install -r app-debug.apk`

## 6. Próximos releases

```powershell
git tag -a v1.0.0 -m "release: MVP Otimiza AI"
git push origin v1.0.0
```

A `deploy.yml` gera AAB release + mapping R8 automaticamente.
