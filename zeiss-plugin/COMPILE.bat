@echo off
REM ========================================================================
REM COMPILACIÓN AUTOMÁTICA - Zeiss Fluorescence Plugin
REM Este script compila el código y genera el JAR final
REM ========================================================================

setlocal enabledelayedexpansion

echo.
echo ========================================================================
echo  COMPILANDO - ZEISS FLUORESCENCE PLUGIN
echo ========================================================================
echo.

REM Verificar Maven
echo [1/4] Verificando Maven...
mvn --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven no se puede ejecutar. Ejecuta SETUP.bat primero.
    pause
    exit /b 1
)
echo [OK] Maven está disponible

REM Limpiar compilaciones previas
echo.
echo [2/4] Limpiando compilaciones anteriores...
mvn clean >nul 2>&1
echo [OK] Limpieza completada

REM Compilar
echo.
echo [3/4] Compilando código Java...
echo (Este paso puede tomar 1-2 minutos en la primera ejecución)
echo.
mvn package -DskipTests
if %errorlevel% neq 0 (
    echo.
    echo ERROR: La compilación falló. Verifica que todos los archivos .java
    echo están en la carpeta correcta.
    echo.
    pause
    exit /b 1
)

REM Verificar si el JAR se generó
echo.
echo [4/4] Verificando JAR generado...
if exist "target\ZeissPlugin_-1.0.0.jar" (
    echo.
    echo ========================================================================
    echo  ¡ÉXITO! El JAR ha sido generado correctamente
    echo ========================================================================
    echo.
    echo Tu archivo está en:
    echo   %cd%\target\ZeissPlugin_-1.0.0.jar
    echo.
    echo PRÓXIMOS PASOS:
    echo   1. Abre Explorer y ve a: target\
    echo   2. Copia el archivo ZeissPlugin_-1.0.0.jar
    echo   3. Pégalo en tu carpeta de Fiji:
    echo      C:\Fiji.app\plugins\
    echo   4. Reinicia Fiji
    echo   5. Ve a Plugins > Zeiss > Zeiss Fluorescence Plugin
    echo.
    pause
) else (
    echo.
    echo ERROR: No se pudo encontrar el JAR generado en target\
    echo Verifica la compilación anterior.
    echo.
    pause
    exit /b 1
)