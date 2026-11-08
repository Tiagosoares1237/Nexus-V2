@echo off
setlocal
cd /d "%~dp0"
title NEXUS - Abrir aplicativo

if not exist "app\NEXUS.jar" goto :sem_arquivo
where javaw >nul 2>&1
if errorlevel 1 goto :sem_java

rem Java instalado: abre sem terminal e sem precisar do VS Code.
start "" javaw -jar "%~dp0app\NEXUS.jar"
exit /b 0

:sem_java
echo.
echo  O Java nao foi encontrado neste computador.
echo  Voce pode instalar o JDK no site https://adoptium.net
 echo  ou usar o EXE portatil, se ja tiver sido criado.
echo.
pause
exit /b 1

:sem_arquivo
echo.
echo  O arquivo app\NEXUS.jar nao foi encontrado.
echo  Extraia o ZIP inteiro antes de abrir o NEXUS.
echo.
pause
exit /b 1
