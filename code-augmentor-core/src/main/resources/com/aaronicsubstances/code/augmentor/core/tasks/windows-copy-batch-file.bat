@echo off
setlocal enabledelayedexpansion
if "%1" == "/Y" set confirmed=1
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
for /F "usebackq tokens=*" %%A in ("%DIRNAME%\CHANGE-SUMMARY.txt") do (
    if defined b (
        set c=%%A
        set /A COUNTER=COUNTER+1
        if defined confirmed (
            set msg="!COUNTER!. copying !b!\!a! to !c!\!a!"
            echo !msg:"=!
            copy "!b!\!a!" "!c!\!a!" /B /Y
            if !ERRORLEVEL! neq 0 (
                exit /B !ERRORLEVEL!
            )
        ) else (
            set msg="!COUNTER!. will copy !b!\!a! to !c!\!a!"
            echo !msg:"=!
        )
        set a=
        set b=
    ) else if defined a (
        set b=%%A
    ) else (
        set a=%%A
    )
)
echo.
if defined confirmed (
    echo Done
) else (
    set msg="NB: Add /Y as first command line arg to actually copy and overwrite"
    echo !msg:"=!
)
endlocal