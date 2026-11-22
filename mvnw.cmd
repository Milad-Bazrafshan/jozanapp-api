@ECHO OFF
SETLOCAL

SET "MAVEN_PROJECTBASEDIR=%~dp0"
SET "MAVEN_HOME=%MAVEN_PROJECTBASEDIR%.mvn\apache-maven-3.9.11"
SET "MAVEN_ZIP=%MAVEN_PROJECTBASEDIR%.mvn\apache-maven-3.9.11-bin.zip"

IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
  IF NOT EXIST "%MAVEN_ZIP%" (
    ECHO Downloading Apache Maven 3.9.11...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip' -OutFile '%MAVEN_ZIP%'"
    IF ERRORLEVEL 1 EXIT /B 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%MAVEN_ZIP%' -DestinationPath '%MAVEN_PROJECTBASEDIR%.mvn' -Force"
  IF ERRORLEVEL 1 EXIT /B 1
)

CALL "%MAVEN_HOME%\bin\mvn.cmd" %*
EXIT /B %ERRORLEVEL%
