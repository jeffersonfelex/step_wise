@echo off
rem Abre um terminal com Java 17 apenas para esta sessao.
setlocal
set "JAVA_HOME=%~dp0..\.tools\java\jdk-17.0.20.1+1"
if not exist "%JAVA_HOME%\bin\javac.exe" (
  echo JDK local ausente. Consulte docs/ambiente-validado.md.
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "GRADLE_USER_HOME=%~dp0..\.tools\gradle-cache"
java -version
javac -version
if /I "%~1"=="--check" exit /b 0
cmd /k
