@echo off
REM ============================================================
REM Publica o Otimiza AI em https://github.com/ramoncontreiras7-alt/OTIMIZA-AI
REM Execute este arquivo com duplo-clique ou:
REM   cd C:\.kilo\worktrees\otimiza-delivery
REM   publicar-otimiza-ai.bat
REM ============================================================

echo.
echo === Otimiza AI - Publicacao no GitHub ===
echo.

cd /d C:\.kilo\worktrees\otimiza-delivery

REM === 1. Verificar git ===
where git >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERRO] Git nao encontrado. Instale em https://git-scm.com/download/win
    pause
    exit /b 1
)

REM === 2. Configurar user.name/email se faltarem ===
git config --global user.name >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo.
    set /p GITNAME="Digite seu NOME (ex: Ramon): "
    git config --global user.name "%GITNAME%"
)
git config --global user.email >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo.
    set /p GITEMAIL="Digite seu EMAIL do GitHub: "
    git config --global user.email "%GITEMAIL%"
)
echo Configurado como: %GITNAME% ^<%GITEMAIL%^>
echo.

REM === 3. Adicionar remote ===
git remote remove origin >nul 2>nul
git remote add origin https://github.com/ramoncontreiras7-alt/OTIMIZA-AI.git
git remote -v
echo.

REM === 4. Renomear branch ===
git branch -M master
echo Branch: master
echo.

REM === 5. Push ===
echo === Fazendo push (ira pedir usuario/senha ou PAT) ===
echo.
echo >> Use seu usuario: ramoncontreiras7-alt
echo >> Use um Personal Access Token (PAT) como senha.
echo >> Nao sabe o que e? https://github.com/settings/tokens/new
echo >> Marque o scope "repo" e clique Generate.
echo.

git push -u origin master
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERRO] Push falhou. Verifique usuario/PAT.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo   SUCESSO! Projeto publicado em:
echo   https://github.com/ramoncontreiras7-alt/OTIMIZA-AI
echo ============================================================
echo.
echo   A CI ira gerar o APK debug em:
echo   Actions -^> Artifacts -^> otimiza-ai-debug-apk
echo.
pause
