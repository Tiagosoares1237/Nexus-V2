@echo off
setlocal EnableExtensions
cd /d "%~dp0"
title NEXUS - Criar aplicativo para Windows

echo.
echo  =============================================
echo   NEXUS - CRIAR EXECUTAVEL DO WINDOWS
 echo  =============================================
echo.
where jpackage >nul 2>&1
if errorlevel 1 goto :sem_jpackage
if not exist "app\NEXUS.jar" goto :sem_jar
if not exist "dist" mkdir dist
if exist "dist\NEXUS" rmdir /s /q "dist\NEXUS"

echo  Preparando o executavel e incluindo o Java...
echo  Esta etapa so precisa ser feita uma vez para cada versao.
echo.
jpackage --type app-image --name NEXUS --input "app" --main-jar NEXUS.jar --main-class ABRIR_NEXUS --app-version 2.0.0 --vendor "NEXUS Workroom" --description "Seu trabalho em um so lugar" --icon "assets\NEXUS.ico" --dest "dist" --add-modules java.desktop,jdk.httpserver
if errorlevel 1 goto :erro

echo.
echo  Criando um ZIP para compartilhar com outras pessoas...
powershell -NoProfile -Command "Compress-Archive -Path 'dist\NEXUS' -DestinationPath 'dist\NEXUS_Windows_Portatil.zip' -Force"
if errorlevel 1 goto :erro

echo.
echo  =============================================
echo   PRONTO! O ARQUIVO ESTARA EM:
echo   dist\NEXUS\NEXUS.exe
 echo  =============================================
echo.
echo  Para enviar a alguem, mande o arquivo:
echo   dist\NEXUS_Windows_Portatil.zip
 echo.
echo  IMPORTANTE: nao envie o EXE sozinho.
echo  Ele precisa das pastas app e runtime que ficam com ele.
echo.
start "" explorer.exe "%~dp0dist\NEXUS"
pause
exit /b 0

:sem_jpackage
echo  Nao encontrei o jpackage. E preciso um JDK 21 ou mais novo.
echo  Confirme a instalacao do JDK e abra este arquivo novamente.
goto :erro
:sem_jar
echo  Arquivo app\NEXUS.jar ausente. Extraia todo o ZIP.
goto :erro
:erro
echo.
echo  Nao foi possivel concluir a criacao do EXE.
echo  Envie uma foto desta janela para identificar a causa.
pause
exit /b 1
