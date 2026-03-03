@echo off
setlocal enabledelayedexpansion

REM Paths (adjust if your project uses different folders)
set ARTIFACT_ROOT=%~dp0..\ci-artifacts
set BUILD_NUM=%BUILD_NUMBER%
if "%BUILD_NUM%"=="" set BUILD_NUM=local-%DATE%-%TIME%
set ARTIFACT_DIR=%ARTIFACT_ROOT%\%BUILD_NUM%

mkdir "%ARTIFACT_DIR%"

echo Running tests...
mvn -Dtestng.xml=testng.xml test
set MVN_EXIT=%ERRORLEVEL%

REM Copy Extent report if present
if exist "test-output\ExtentReport.html" (
  copy /Y "test-output\ExtentReport.html" "%ARTIFACT_DIR%\ExtentReport.html"
)

REM Copy surefire reports
if exist "target\surefire-reports" (
  xcopy /E /I /Y "target\surefire-reports" "%ARTIFACT_DIR%\surefire-reports\"
)

REM Copy screenshots and logs if present
if exist "screenshots" (
  xcopy /E /I /Y "screenshots" "%ARTIFACT_DIR%\screenshots\"
)
if exist "logs" (
  xcopy /E /I /Y "logs" "%ARTIFACT_DIR%\logs\"
)

echo Artifacts copied to %ARTIFACT_DIR%
exit /b %MVN_EXIT%