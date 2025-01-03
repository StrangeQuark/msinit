@echo off
:: Extract all zip files in the current folder
for %%f in (*.zip) do (
    echo Extracting %%f...
    powershell -Command "Expand-Archive -Force %%f ."
)

:: Create the docker network
echo Creating docker network for Genesis
docker network create genesis-network

:: Delete the zip files
for %%f in (*.zip) do (
    echo Deleting %%f...
    del %%f
)

:: Run python script and delete afterwards
python script.py
del script.py

:: Delete this batch file
echo Deleting this script...
cmd /c del "%~f0"

echo All files extracted and zip files deleted!
pause