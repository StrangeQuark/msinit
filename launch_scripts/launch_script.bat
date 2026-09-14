@echo off
setlocal enabledelayedexpansion

set networks=shared-network
set "foundTest=0"
set "foundLogger=0"
set "foundTelemetry=0"
set "foundAuth=0"
set "foundEmail=0"
set "foundFile=0"
set "foundVault=0"
set "foundReact=0"
set "foundGateway=0"

for /D %%F in (*) do (
    echo %%~nxF | findstr /I /B /C:"jenkinsservice" >nul && set networks=!networks! jenkins-network
    echo %%~nxF | findstr /I /B /C:"testservice" >nul && (
        set "foundTest=1"
        set "testFolder=%%F"
    )
    echo %%~nxF | findstr /I /B /C:"loggerservice" >nul && (
        set "foundLogger=1"
        set "loggerFolder=%%F"
    )
    echo %%~nxF | findstr /I /B /C:"telemetryservice" >nul && (
        set "foundTelemetry=1"
        set "telemetryFolder=%%F"
    )
    echo %%~nxF | findstr /I /B /C:"authservice" >nul && set "foundAuth=1"
    echo %%~nxF | findstr /I /B /C:"emailservice" >nul && set "foundEmail=1"
    echo %%~nxF | findstr /I /B /C:"fileservice" >nul && set "foundFile=1"
    echo %%~nxF | findstr /I /B /C:"vaultservice" >nul && set "foundVault=1"
    echo %%~nxF | findstr /I /B /C:"reactservice" >nul && set "foundReact=1"
    echo %%~nxF | findstr /I /B /C:"gatewayservice" >nul && set "foundGateway=1"
)

for %%N in (%networks%) do (
    docker network ls --format "{{.Name}}" | findstr /R /C:"^%%N$" >nul
    if errorlevel 1 (
        docker network create %%N
        echo Created network: %%N
    ) else (
        echo Network already exists: %%N
    )
)

for /D %%F in (*) do (
    set "skip=0"
    echo %%~nxF | findstr /I /B /C:"testservice" >nul && set "skip=1"
    echo %%~nxF | findstr /I /B /C:"loggerservice" >nul && set "skip=1"

    if !skip!==0 if exist "%%F\docker-compose.yml" (
        echo Starting docker-compose in %%F
        set "testEnvFile="
        if !foundTest!==1 (
            echo %%~nxF | findstr /I /B /C:"authservice" >nul && set "testEnvFile=--env-file .env.test"
            echo %%~nxF | findstr /I /B /C:"gatewayservice" >nul && set "testEnvFile=--env-file .env.test"
        )
        start "%%~nxF" cmd /c "cd /d %%F && docker-compose !testEnvFile! up --build"
    )
)

if !foundTest!==0 (
    if !foundLogger!==1 start "loggerservice" cmd /c "cd /d !loggerFolder! && docker-compose up --build"
    goto :EOF
)

if !foundAuth!==1 call :WaitForHealthy auth-service
if !foundEmail!==1 call :WaitForHealthy email-service
if !foundFile!==1 call :WaitForHealthy file-service
if !foundVault!==1 call :WaitForHealthy vault-service
if !foundReact!==1 call :WaitForHealthy react-service
if !foundGateway!==1 call :WaitForHealthy gateway-service

pushd "!testFolder!"
call docker-compose up --build --abort-on-container-exit
set "testExitCode=!errorlevel!"
call docker-compose logs --no-color > test-service.log
call docker-compose down -v
popd

if not !testExitCode! EQU 0 (
    echo Testservice FAILED with exit code !testExitCode!
    goto :Teardown
)

if !foundLogger!==1 start "loggerservice" cmd /c "cd /d !loggerFolder! && docker-compose up --build"
if !foundLogger!==1 call :WaitForHealthy logger-service

echo Testservice passed.
goto :EOF

:WaitForHealthy
set "keyword=%~1"
set "maxRetries=150"
set "retryCount=0"

:HealthLoop
set "containerName="
for /f "tokens=*" %%i in ('docker ps -a --format "{{.Names}}" ^| findstr /I "%keyword%"') do (
    set "containerName=%%i"
    goto :FoundContainer
)

:FoundContainer
if not defined containerName goto :HealthRetry

for /f "tokens=*" %%h in ('docker inspect --format "{{.State.Health.Status}}" "%containerName%" 2^>nul') do set "healthStatus=%%h"
if /I "%healthStatus%"=="healthy" exit /b 0

:HealthRetry
set /a retryCount+=1
if %retryCount% GEQ %maxRetries% exit /b 1
timeout /t 2 /nobreak >nul
goto :HealthLoop

:Teardown
for /D %%F in (*) do (
    if exist "%%F\docker-compose.yml" (
        pushd "%%F"
        call docker-compose down -v
        popd
    )
)
exit /b %testExitCode%
