@echo off
setlocal
set JAVA_HOME=C:\Program Files\Java\jdk-23
set PATH=C:\tools\apache-maven-3.9.9\bin;%PATH%
cd /d "%~dp0"
mvn clean package %*
