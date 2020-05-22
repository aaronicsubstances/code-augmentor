@echo off
setlocal enabledelayedexpansion
if "%1" == "-f" set confirmed=1
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
for /F "usebackq tokens=*" %%A in ("%DIRNAME%\CHANGE-SUMMARY.txt") do (
    if defined b (
        set c=%%A
        set /A COUNTER=COUNTER+1
        if defined confirmed (
            echo !COUNTER!. copying !c!\!a! to !b!\!a!
            copy "!c!\!a!" "!b!\!a!" /B /Y
            if !ERRORLEVEL! neq 0 (
                exit /B !ERRORLEVEL!
            )
        ) else (
            echo !COUNTER!. will copy !c!\!a! to !b!\!a!
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
    echo NB: Add -f as first command line arg to actually copy and overwrite
)
endlocal