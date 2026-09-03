# =============================================================================
# init-github.ps1 — Publica o projeto Otimiza AI no GitHub como "OTIMIZA-AI"
# Uso (ja executado, este arquivo fica como referencia):
#   1. Crie o repo VAZIO em https://github.com/new com o nome "OTIMIZA-AI"
#   2. Edite $GithubUser e (opcional) $RepoName abaixo
#   3. Execute: powershell -ExecutionPolicy Bypass -File .\init-github.ps1
# =============================================================================

$ErrorActionPreference = "Stop"

# ---------- CONFIGURAR AQUI ----------
$GithubUser = "ramoncontreiras7-alt"
$RepoName   = "OTIMIZA-AI"             # nome EXATO do repo criado no GitHub (case-sensitive)
$Branch     = "master"                 # main ou master, conforme o default do seu GitHub
$Visibility = "public"                 # public | private
$UseSsh     = $true                    # $true = SSH (recomendado); $false = HTTPS+PAT
# ------------------------------------

$Worktree = "C:\.kilo\worktrees\otimiza-delivery"

if (-not (Test-Path $Worktree)) {
    Write-Error "Worktree nao encontrado: $Worktree"
    exit 1
}
Set-Location $Worktree

Write-Host "`n[1/6] Verificando Git..." -ForegroundColor Cyan
$git = Get-Command git -ErrorAction SilentlyContinue
if (-not $git) { Write-Error "Git nao instalado"; exit 1 }
& git --version

Write-Host "`n[2/6] Configurando identidade (se vazia)..." -ForegroundColor Cyan
$name  = & git config --global user.name
$email = & git config --global user.email
if ([string]::IsNullOrWhiteSpace($name)) {
    $name = Read-Host "Seu nome (ex: Joao da Silva)"
    & git config --global user.name $name
}
if ([string]::IsNullOrWhiteSpace($email)) {
    $email = Read-Host "Seu email GitHub (ex: joao@email.com)"
    & git config --global user.email $email
}
Write-Host "  user.name  = $name"
Write-Host "  user.email = $email"

Write-Host "`n[3/6] Configurando remote..." -ForegroundColor Cyan
if ($UseSsh) {
    $RemoteUrl = "git@github.com:${GithubUser}/${RepoName}.git"
} else {
    $RemoteUrl = "https://github.com/${GithubUser}/${RepoName}.git"
}
& git remote remove origin 2>$null
& git remote add origin $RemoteUrl
& git remote -v

Write-Host "`n[4/6] Verificando autenticacao..." -ForegroundColor Cyan
if ($UseSsh) {
    $keyPath = "$env:USERPROFILE\.ssh\id_ed25519"
    if (-not (Test-Path $keyPath)) {
        Write-Host "  Gerando chave SSH ed25519..." -ForegroundColor Yellow
        New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.ssh" | Out-Null
        & ssh-keygen -t ed25519 -f $keyPath -N '""' -C $email
    }
    $pubKey = Get-Content "${keyPath}.pub"
    Write-Host "`n  >>> ADICIONE ESTA CHAVE NO GITHUB <<<" -ForegroundColor Green
    Write-Host "  https://github.com/settings/keys -> New SSH key" -ForegroundColor Green
    Write-Host "  ----------------------------------------" -ForegroundColor Green
    Write-Host $pubKey
    Write-Host "  ----------------------------------------" -ForegroundColor Green
    Write-Host "  Pressione ENTER apos adicionar a chave no GitHub..." -ForegroundColor Yellow
    Read-Host | Out-Null
    & ssh -T git@github.com 2>&1 | Out-String | Write-Host
} else {
    Write-Host "  Para HTTPS, voce precisara de um Personal Access Token (PAT)" -ForegroundColor Yellow
    Write-Host "  https://github.com/settings/tokens -> Generate new token (classic)" -ForegroundColor Yellow
    Write-Host "  Scopes: repo (todas), workflow" -ForegroundColor Yellow
    $pat = Read-Host "  Cole o PAT (nao sera exibido)" -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($pat)
    $plain = [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    if ($plain) {
        $RemoteUrlWithPat = "https://${GithubUser}:${plain}@github.com/${GithubUser}/${RepoName}.git"
        & git remote set-url origin $RemoteUrlWithPat
    }
}

Write-Host "`n[5/6] Renomeando branch para $Branch..." -ForegroundColor Cyan
& git branch -M $Branch 2>&1 | Out-Null

Write-Host "`n[6/6] Push para GitHub..." -ForegroundColor Cyan
& git push -u origin $Branch

if ($LASTEXITCODE -eq 0) {
    $repoUrl = "https://github.com/${GithubUser}/${RepoName}"
    Write-Host "`n✅ PROJETO PUBLICADO COM SUCESSO!" -ForegroundColor Green
    Write-Host "   URL: $repoUrl" -ForegroundColor Green
    Write-Host "`nProximo passo: a CI (Actions) vai gerar o APK debug como artifact." -ForegroundColor Cyan
    Write-Host "   Acesse: $repoUrl/actions" -ForegroundColor Cyan
} else {
    Write-Host "`n❌ Push falhou. Verifique credenciais/URL e tente novamente." -ForegroundColor Red
    exit 1
}