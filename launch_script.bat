@echo off
setlocal enabledelayedexpansion

REM --- CONFIG ---
set networks=shared-network authdb-network emaildb-network filedb-network vaultdb-network

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
