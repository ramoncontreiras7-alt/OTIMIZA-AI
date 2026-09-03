# Publicação do Otimiza AI no GitHub

> **Repositório criado:** `https://github.com/ramoncontreiras7-alt/OTIMIZA-AI`
>
> ⚠️ O nome do repo está em **CAIXA ALTA** (`OTIMIZA-AI`) — o GitHub preserva case na URL.
> O nome do pacote Android segue sendo `com.otimiza.delivery` (FQCN), apenas o **repo** muda.

## 1. Repositório criado

✅ Repo já criado: **https://github.com/ramoncontreiras7-alt/OTIMIZA-AI**

⚠️ Não faça mais nada no GitHub — vamos apenas fazer o **push** a partir do worktree.

## 2. Configurar suas credenciais no script

Abra `init-github.ps1` (na raiz do projeto) e edite **apenas** o bloco de configuração:

```powershell
$GithubUser = "ramoncontreiras7-alt"   # ← seu user (já correto)
$RepoName   = "OTIMIZA-AI"             # já está correto (case-sensitive)
$Branch     = "master"                 # ou "main" se preferir
$UseSsh     = $true                    # true = SSH; false = PAT
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
https://github.com/ramoncontreiras7-alt/OTIMIZA-AI
```

Você deve ver:
- 3 commits (`feat: scaffold…`, `release: pipeline estatico…`, `docs: publish as 'otimiza-ai'…`)
- 30+ classes Kotlin
- Workflows `.github/workflows/ci.yml` e `deploy.yml`
- `init-github.ps1`, `ARCHITECTURE.md`, `RELEASE.md`, `CHANGELOG.md`, `PUBLISH.md`

## 5. Baixar o APK debug

Acesse https://github.com/ramoncontreiras7-alt/OTIMIZA-AI/actions
- Clique no primeiro workflow **CI - Otimiza AI** (verde)
- Role até **Artifacts** → `otimiza-ai-debug-apk` → Download
- Instale no celular: `adb install -r app-debug.apk`

## 6. Próximos releases

```powershell
git tag -a v1.0.0 -m "release: MVP Otimiza AI"
git push origin v1.0.0
```

A `deploy.yml` gera AAB release + mapping R8 automaticamente.
