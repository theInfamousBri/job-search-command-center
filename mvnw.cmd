@echo off
setlocal EnableExtensions

set "BASE_DIR=%~dp0"
set "PROPERTIES=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"

if not exist "%PROPERTIES%" (
  echo Error: %PROPERTIES% is missing. 1>&2
  exit /b 1
)

set "DIST_URL="
for /f "usebackq tokens=1,* delims==" %%A in ("%PROPERTIES%") do (
  if "%%A"=="distributionUrl" set "DIST_URL=%%B"
)

if "%DIST_URL%"=="" (
  echo Error: distributionUrl is not configured in %PROPERTIES%. 1>&2
  exit /b 1
)

for %%F in ("%DIST_URL%") do set "ARCHIVE_NAME=%%~nxF"
set "MAVEN_VERSION=%ARCHIVE_NAME:apache-maven-=%"
set "MAVEN_VERSION=%MAVEN_VERSION:-bin.zip=%"

if "%MAVEN_USER_HOME%"=="" set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "WRAPPER_HOME=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_HOME=%WRAPPER_HOME%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

if exist "%MAVEN_CMD%" goto runMaven

if not exist "%WRAPPER_HOME%" mkdir "%WRAPPER_HOME%"
set "ARCHIVE=%TEMP%\maven-wrapper-%RANDOM%-%ARCHIVE_NAME%"

echo Downloading Apache Maven %MAVEN_VERSION%... 1>&2

rem Pass paths through environment variables so PowerShell handles spaces safely
rem and batch does not expand values before they are assigned.
set "MVNW_DIST_URL=%DIST_URL%"
set "MVNW_ARCHIVE=%ARCHIVE%"
set "MVNW_WRAPPER_HOME=%WRAPPER_HOME%"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop'; $ProgressPreference='SilentlyContinue'; try { Invoke-WebRequest -UseBasicParsing -Uri $env:MVNW_DIST_URL -OutFile $env:MVNW_ARCHIVE; Expand-Archive -LiteralPath $env:MVNW_ARCHIVE -DestinationPath $env:MVNW_WRAPPER_HOME -Force } finally { if (Test-Path -LiteralPath $env:MVNW_ARCHIVE) { Remove-Item -LiteralPath $env:MVNW_ARCHIVE -Force } }"
set "WRAPPER_EXIT=%ERRORLEVEL%"

set "MVNW_DIST_URL="
set "MVNW_ARCHIVE="
set "MVNW_WRAPPER_HOME="

if not "%WRAPPER_EXIT%"=="0" exit /b %WRAPPER_EXIT%

if not exist "%MAVEN_CMD%" (
  echo Error: Maven Wrapper could not install Maven %MAVEN_VERSION%. 1>&2
  exit /b 1
)

:runMaven
call "%MAVEN_CMD%" %*
exit /b %ERRORLEVEL%
