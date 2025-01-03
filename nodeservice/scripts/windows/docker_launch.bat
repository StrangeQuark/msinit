@echo off
:: Loop through all directories (subdirectories only) in the current folder
for /d %%d in (*) do (
    :: Check if a docker-compose.yml file exists in the directory
    if exist "%%d/docker-compose.yml" (
        echo Running docker-compose up -d in directory: %%d
        cd "%%d"
        docker-compose up -d
        cd ..
    ) else (
        echo Skipping directory: %%d (No docker-compose.yml found)
    )
)

echo All docker-compose commands executed.
pause