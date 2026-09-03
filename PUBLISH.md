# Publicação do Otimiza AI no GitHub

> **Repositório criado:** https://github.com/ramoncontreiras7-alt/OTIMIZA-AI
>
> ⚠️ O nome do repo está em **CAIXA ALTA** (`OTIMIZA-AI`) — o GitHub preserva case na URL.
> O nome do pacote Android segue sendo `com.otimiza.delivery` (FQCN), apenas o **repo** muda.

## 1. Repositório criado

✅ Repo já criado: **https://github.com/ramoncontreiras7-alt/OTIMIZA-AI**

⚠️ Não faça mais nada no GitHub — o push já foi feito. Esta página é só referência futura.

## 2. Comandos executados (referência)

```powershell
cd C:\.kilo\worktrees\otimiza-delivery
git remote add origin https://github.com/ramoncontreiras7-alt/OTIMIZA-AI.git
git branch -M master
git push -u origin master
```

## 3. Próximas publicações

Para atualizações futuras:

```powershell
cd C:\.kilo\worktrees\otimiza-delivery
git add -A
git commit -m "sua mensagem"
git push
```

Para um release versionado (gera AAB + mapping):

```powershell
git tag -a v1.0.0 -m "release: MVP Otimiza AI"
git push origin v1.0.0
```

## 4. Verificar o repositório

URL: https://github.com/ramoncontreiras7-alt/OTIMIZA-AI

Você deve ver:
- 4 commits
- 30+ classes Kotlin
- 7 testes unitários
- Workflows `.github/workflows/ci.yml` e `deploy.yml`
- `init-github.ps1`, `publicar-otimiza-ai.bat`, `ARCHITECTURE.md`, `RELEASE.md`, `CHANGELOG.md`, `PUBLISH.md`

## 5. Baixar o APK debug

Acesse https://github.com/ramoncontreiras7-alt/OTIMIZA-AI/actions
- Clique no workflow **CI - Otimiza AI** (verde)
- Role até **Artifacts** → `otimiza-ai-debug-apk` → Download
- Instale no celular: `adb install -r app-debug.apk`