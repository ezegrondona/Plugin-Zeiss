@echo off
REM ========================================================================
REM SETUP AUTOMÁTICO - Zeiss Fluorescence Plugin
REM Este script prepara todo para compilar el plugin
REM ========================================================================

setlocal enabledelayedexpansion

echo.
echo ========================================================================
echo  SETUP - ZEISS FLUORESCENCE PLUGIN PARA FIJI
echo ========================================================================
echo.

REM Verificar si Maven está instalado
echo [1/3] Verificando Maven...
mvn --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven no está instalado o no está en el PATH
    echo Por favor, asegúrate de que:
    echo   1. Ejecutaste: choco install maven
    echo   2. Reiniciaste la consola después de instalar
    echo   3. Maven está en las variables de entorno del sistema
    echo.
    pause
    exit /b 1
) else (
    echo [OK] Maven encontrado
)

echo.
echo [2/3] Creando estructura de carpetas...

REM Crear estructura de carpetas
if not exist "src\main\java\com\microscopia\zeiss" (
    mkdir src\main\java\com\microscopia\zeiss
    echo [OK] Carpetas creadas
) else (
    echo [OK] Carpetas ya existen
)

echo.
echo [3/3] Verificando archivos necesarios...

REM Verificar si existen los archivos .java
set missing=0

if not exist "src\main\java\com\microscopia\zeiss\ZeissPlugin.java" (
    echo ERROR: Falta ZeissPlugin.java
    set missing=1
)
if not exist "src\main\java\com\microscopia\zeiss\DialogManager.java" (
    echo ERROR: Falta DialogManager.java
    set missing=1
)
if not exist "src\main\java\com\microscopia\zeiss\FluorescenceAnalyzer.java" (
    echo ERROR: Falta FluorescenceAnalyzer.java
    set missing=1
)
if not exist "src\main\java\com\microscopia\zeiss\ChannelProcessor.java" (
    echo ERROR: Falta ChannelProcessor.java
    set missing=1
)
if not exist "pom.xml" (
    echo ERROR: Falta pom.xml
    set missing=1
)

if %missing% equ 1 (
    echo.
    echo INSTRUCCIONES: Copia los siguientes archivos a esta carpeta:
    echo   - pom.xml
    echo   - src\main\java\com\microscopia\zeiss\ZeissPlugin.java
    echo   - src\main\java\com\microscopia\zeiss\DialogManager.java
    echo   - src\main\java\com\microscopia\zeiss\FluorescenceAnalyzer.java
    echo   - src\main\java\com\microscopia\zeiss\ChannelProcessor.java
    echo.
    pause
    exit /b 1
) else (
    echo [OK] Todos los archivos están presentes
)

echo.
echo ========================================================================
echo  SETUP COMPLETADO - Ahora ejecuta COMPILE.bat
echo ========================================================================
echo.
pause