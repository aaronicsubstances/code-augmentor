@echo off
set args=%1
shift
:start
if [%1] == [] goto done
set args=%args% %1
shift
goto start

:done
rem (use %args% here)
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set grapedir=%DIRNAME%\build
IF EXIST "%grapedir%" (
    rmdir /S /Q "%grapedir%"
)
groovy "-Dgrape.root=%grapedir%" %args%