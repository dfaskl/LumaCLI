@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
set JAVA_HOME=C:\Program Files\Java\jdk-23
set PATH=C:\tools\apache-maven-3.9.9\bin;%PATH%
java --enable-native-access=ALL-UNNAMED -Dlumacli.snapshot.enabled=false -jar target\lumacli-1.0-SNAPSHOT.jar %*
