@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title NEXUS - Compilar Java
if not exist "build\classes" mkdir "build\classes"
if not exist "build\recursos\app" mkdir "build\recursos\app"
where javac >nul 2>&1
if errorlevel 1 goto :erro
where jar >nul 2>&1
if errorlevel 1 goto :erro

echo [1/3] Compilando...
javac -encoding UTF-8 -d "build\classes" ABRIR_NEXUS.java TESTAR_NEXUS.java
if errorlevel 1 goto :erro

echo [2/3] Testando...
java -cp "build\classes" TESTAR_NEXUS
if errorlevel 1 goto :erro

echo [3/3] Atualizando o app\NEXUS.jar...
copy /y "assets\nexus-tray.png" "build\recursos\app\nexus-tray.png" >nul
jar --create --file "app\NEXUS.jar" --main-class ABRIR_NEXUS -C "build\classes" . -C "." web -C "build\recursos" app
if errorlevel 1 goto :erro

echo.
echo Pronto! Agora abra 01_ABRIR_NEXUS.bat.
pause
exit /b 0
:erro
echo.
echo Falha ao compilar. Leia o erro acima.
pause
exit /b 1
