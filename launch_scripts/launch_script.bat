@REM @echo off
@REM setlocal enabledelayedexpansion

@REM REM --- CONFIG ---
@REM set networks=shared-network
@REM set networks=!networks! authdb-network  REM Integration line: Auth
@REM set networks=!networks! emaildb-network REM Integration line: Email
@REM set networks=!networks! filedb-network  REM Integration line: File
@REM set networks=!networks! vaultdb-network REM Integration line: Vault
@REM set networks=!networks! telemetrydb-network REM Integration line: Telemetry

@REM REM --- Create missing networks ---
@REM for %%N in (%networks%) do (
@REM     docker network ls --format "{{.Name}}" | findstr /R /C:"^%%N$" >nul
@REM     if errorlevel 1 (
@REM         echo Creating network %%N
@REM         docker network create %%N
@REM     ) else (
@REM         echo Network already exists: %%N
@REM     )
@REM )

@REM REM --- Iterate through subfolders and run docker-compose ---
@REM for /D %%F in (*) do (
@REM     if exist "%%F\docker-compose.yml" (
@REM         echo Starting docker-compose in %%F
@REM         start cmd /c "cd /d %%F && docker-compose up --build"
@REM     )
@REM )

@REM endlocal

@echo off
setlocal enabledelayedexpansion

REM --- CONFIG ---
set networks=shared-network
set networks=!networks! authdb-network REM Integration line: Auth
set networks=!networks! emaildb-network REM Integration line: Email
set networks=!networks! filedb-network REM Integration line: File
set networks=!networks! vaultdb-network REM Integration line: Vault
set networks=!networks! telemetrydb-network REM Integration line: Telemetry

REM --- Flags ---
set "foundTest=0"
set "foundLogger=0"
set "foundTelemetry=0"
set "testFolder="
set "loggerFolder="
set "telemetryFolder="

REM --- Create missing networks ---
for %%N in (%networks%) do (
    docker network ls --format "{{.Name}}" | findstr /R /C:"^%%N$" >nul
    if errorlevel 1 (
        echo Creating network %%N
        docker network create %%N
    ) else (
        echo Network already exists: %%N
    )
)

REM --- Iterate through subfolders and run docker-compose ---
for /D %%F in (*) do (
    REM Integration function start: Test
    set "skip=0"
    if /I "%%~nxF"=="testservice" (
        set "skip=1"
        set "foundTest=1"
        set "testFolder=%%F"
    )
    REM Integration function start: Logger
    if /I "%%~nxF"=="loggerservice" (
        set "skip=1"
        set "foundLogger=1"
        set "loggerFolder=%%F"
    )
    REM Integration function end: Logger
    REM Integration function start: Telemetry
    if /I "%%~nxF"=="telemetryservice" (
        set "skip=1"
        set "foundTelemetry=1"
        set "telemetryFolder=%%F"
    )
    REM Integration function end: Telemetry

    if !skip!==0 ( REM Integration function end: Test
        if exist "%%F\docker-compose.yml" (
            echo Starting docker-compose in %%F
            start "%%~nxF" cmd /c "cd /d %%F && docker-compose up --build"
        )
    ) REM Integration line: Test
)

REM --- PRE-TEST HEALTH CHECKS ---
if !foundTest!==1 (
    set "wait_list=auth-service email-service file-service vault-service react-service gateway-service"

    for %%W in (%wait_list%) do (
        call :WaitForHealthy %%W
        if !errorlevel! NEQ 0 (
            echo [CRITICAL] Dependency %%W failed to become healthy. Aborting.
            goto :Teardown
        )
    )
    
    pushd "!testFolder!"
    
    call docker-compose up --build --abort-on-container-exit
    
    set "testExitCode=!errorlevel!"

    echo Saving test output to test-service.log...
    call docker-compose logs --no-color > test-service.log

    if !testExitCode! EQU 0 (
        echo [SUCCESS] Testservice passed.
        
        REM Explicitly tear down the test container (Matching Shell Script)
        echo Tearing down test-service container...
        call docker-compose down -v
        popd

        REM --- Launch Final Services ---
        if !foundLogger!==1 (
            echo Starting logger service
            start "loggerservice" cmd /c "cd /d !loggerFolder! && docker-compose up --build"
        )
        
        if !foundTelemetry!==1 (
            echo Starting telemetry service
            start "telemetryservice" cmd /c "cd /d !telemetryFolder! && docker-compose up --build"
        )

        REM --- FINAL HEALTH CHECKS ---
        echo Waiting for Logger and Telemetry to be healthy...
        call :WaitForHealthy "logger-service"
        if !errorlevel! NEQ 0 goto :Teardown

        call :WaitForHealthy "telemetry-service"
        if !errorlevel! NEQ 0 goto :Teardown

        echo.
        echo [DONE] Exit code: !testExitCode!
        exit /b !testExitCode!

    ) else (
        popd
        echo.
        echo [FAILURE] Testservice FAILED with exit code !testExitCode!
        echo Initiating global teardown...
        goto :Teardown
    )

) else (
    echo [INFO] No Test Service found. Skipping test phase.
)

goto :EOF


REM =========================================================
REM FUNCTIONS
REM =========================================================

:WaitForHealthy
set "keyword=%~1"
set "max_retries=60" 
set "retry_count=0"

echo Waiting for container containing keyword '%keyword%'...

:HealthLoop
    REM 1. Find Container Name based on keyword
    set "container_name="
    for /f "tokens=*" %%i in ('docker ps -a --format "{{.Names}}" ^| findstr /I "%keyword%"') do (
        set "container_name=%%i"
        goto :FoundContainer
    )

    :FoundContainer
    if not defined container_name (
        echo Container matching '%keyword%' not found yet...
        goto :HealthRetry
    )

    echo Container matching '%keyword%' was found

    REM 2. Check Health Status
    set "health_status="
    for /f "tokens=*" %%h in ('docker inspect --format "{{.State.Health.Status}}" "%container_name%" 2^>nul') do (
        set "health_status=%%h"
    )
    
    echo Container matching '%keyword%' state is '%health_status%'

    if /I "%health_status%"=="healthy" (
        echo %container_name% is healthy.
        exit /b 0
    )

    if "%health_status%"=="" (
        echo %container_name% has no healthcheck or is not ready.
    )

    :HealthRetry
    set /a retry_count+=1
    if %retry_count% GEQ %max_retries% (
        echo Timed out waiting for container '%container_name%' with keyword '%keyword%'.
        exit /b 1
    )

    REM Wait 5 seconds (Shell script waits 2, but Batch loops are tighter, 2-5 is safe)
    timeout /t 2 /nobreak >nul
    goto :HealthLoop


REM =========================================================
REM TEARDOWN
REM =========================================================
:Teardown
echo.
echo Tearing down all docker containers
for /D %%F in (*) do (
    if exist "%%F\docker-compose.yml" (
        pushd "%%F"
        REM Added --rmi all to match shell script strictness
        call docker-compose down -v --rmi all
        popd
    )
)
REM Exit with failure code (assuming failure if we reached here via goto)
exit /b 1