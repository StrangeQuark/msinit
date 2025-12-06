@echo off
setlocal enabledelayedexpansion

REM --- CONFIG ---
set networks=shared-network
set networks=!networks! authdb-network  REM Integration line: Auth
set networks=!networks! emaildb-network REM Integration line: Email
set networks=!networks! filedb-network  REM Integration line: File
set networks=!networks! vaultdb-network REM Integration line: Vault
set networks=!networks! telemetrydb-network REM Integration line: Telemetry

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
    if exist "%%F\docker-compose.yml" (
        echo Starting docker-compose in %%F
        start cmd /c "cd /d %%F && docker-compose up --build"
    )
)

endlocal
