@echo off
setlocal enabledelayedexpansion

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.

set CMDNAME=%~n0

if "%1" == "-h" (
    goto help
)

if "%1" == "--rev" (
    set reverseFileNames=1
    shift
)

set args=%1
shift
:start
if [%1] == [] goto done
set args=%args% %1
shift
goto start

:done
rem can use %args% now

set arbitraryCommand=%args%
if "%arbitraryCommand%" == "-f" (
    set confirmed=1
    set arbitraryCommand=
)

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
        ) else if defined arbitraryCommand (
            if defined reverseFileNames (
                !arbitraryCommand! !c!\!a! !b!\!a!
            ) else (
                !arbitraryCommand! !b!\!a! !c!\!a!
            )
        ) else (
            echo !COUNTER!. !a!
        )
        set a=
        set b=
    ) else if defined a (
        set b=%%A
    ) else (
        set a=%%A
    )
)

:help
if defined confirmed (
    echo.
    echo Done
) else if defined arbitraryCommand (
    rem Done
) else (
    echo.
    echo Usage: %CMDNAME% [-f ^| -h ^| [--rev] ^<arbitraryCommandWithArgs^>]
    echo where
    echo     -f                              use to actually copy and overwrite
    echo.
    echo     -h                              prints this help information and exits
    echo.
    echo     ^<arbitraryCommandWithArgs^>      use to specify a command which will be called iteratively 
    echo                                     with each source file and its corresponding generated file 
    echo                                     appended to its args. Can prepend command with --rev to 
    echo                                     rather have generated file appended before corresponding
    echo                                     source file. Exit value of command invocation in each iteration
    echo                                     is NOT checked.
    echo.
    echo NB: If no arguments are specified, only relative paths of source files are printed and then 
    echo this help message is printed afterwards.
    echo.
    echo EXAMPLES
    echo.
    echo 1. Use
    echo       %CMDNAME% -f
    echo    to overwrite each source file with its corresponding generated file to achieve synchronization
    echo    goal of Code Augmentor.
    echo.
    echo 2. Can use
    echo       %CMDNAME% echo
    echo    to view full paths of each changed source file and its corresponding generated file on a line.
    echo.
    echo 3. Can use
    echo       %CMDNAME% fc /n /t
    echo    to use built-in File Compare program on Windows to view file differences ^(it assumes files are in 
    echo    ASCII encoding^)
    echo. 
    echo 4. Can use
    echo       %CMDNAME% git diff
    echo    to view file differences with Git and get coloured console output.
    echo. 
    echo 5. Can use
    echo       %CMDNAME% git --no-pager diff ^> diff.txt
    echo    to save file differences with Git.
    echo.
    echo 6. Can use
    echo       %CMDNAME% --rev copy /B /Y
    echo    to achieve similar effect as %CMDNAME% -f ^(but without error checking^)
    echo.
)
endlocal