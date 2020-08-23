@echo off
setlocal enabledelayedexpansion

rem 1. Specify the command to execute and get the latest version.
set EXPECTED_VERSION_COMMAND=git rev-parse master

rem 2. Receive path to source file for recording latest version as first 
rem    command line argument.
rem NB: empty lines will be ignored.
set ACTUAL_VERSION_PATH=%1
if exist "%1" (
    rem ok, file exists.
) else (
    if "!ACTUAL_VERSION_PATH!" == "" (
        echo Path to source file for recording latest version is required as first command line argument 1>&2
    ) else (
        echo Provided path to source file for recording latest version does not exist: !ACTUAL_VERSION_PATH! 1>&2
    )
    exit /B 1
)

rem 3. Add non empty header lines which precede latest version information.
set EXPECTED_SIZE=0
set EXPECTED[!EXPECTED_SIZE!]=H
set /a EXPECTED_SIZE+=1

rem 4. Now execute command and get lines of STDOUT as latest version information.
rem NB: for loop doesn't set errorlevel with failure code, so fetch STDERR as 
rem     well, which will likely signal the problem at a point after this script is run.
rem NB: Observed that java -version and javac -version write to STDERR only.
for /F "tokens=*" %%A in ('%EXPECTED_VERSION_COMMAND% 2^>^&1') do (
    rem since file reading skips empty lines do same here too
    if "%%A" neq "" (
        set EXPECTED[!EXPECTED_SIZE!]=%%A
        set /a "EXPECTED_SIZE+=1"
    )
)

rem 5. Add non empty trailing lines which follow latest version information.
set EXPECTED[%EXPECTED_SIZE%]=T
set /a EXPECTED_SIZE+=1

rem 6. Confirm all lines have been gathered correctly.
echo Expected lines from "%EXPECTED_VERSION_COMMAND%":
set I=0
:loop_start_1
if !I! == !EXPECTED_SIZE! (
    goto loop_end_1
)
set /a diffLnNum=I+1 
echo %diffLnNum%. !EXPECTED[%I%]!
set /a I+=1
goto loop_start_1
:loop_end_1

rem 7. Now gather the actual version file contents. No extra work needed to work with 
rem    Unix line endings.
rem    NB: empty lines are skipped by for loop when fetching file contents.
set ACTUAL_SIZE=0
for /F "usebackq tokens=*" %%A in ("%ACTUAL_VERSION_PATH%") do (
    set ACTUAL[!ACTUAL_SIZE!]=%%A
    set /a "ACTUAL_SIZE+=1"
)

echo.
echo Actual lines read from "%ACTUAL_VERSION_PATH%": 
set I=0
:loop_start_2
if !I! == !ACTUAL_SIZE! (
    goto loop_end_2
)
set /a diffLnNum=I+1 
echo %diffLnNum%. !ACTUAL[%I%]!
set /a I+=1
goto loop_start_2
:loop_end_2

rem 8. Now compare actual file contents against expected for equality.
echo.
if !ACTUAL_SIZE! neq !EXPECTED_SIZE! (
    echo Difference detected in line counts ^(actual vrs expected^): !ACTUAL_SIZE! neq !EXPECTED_SIZE!
    goto comparison_complete
)

set I=0
:loop_start_3
if !I! == !ACTUAL_SIZE! (
    goto loop_end_3
)
set actualLine=!ACTUAL[%I%]!
set expectedLine=!EXPECTED[%I%]!
if "!actualLine!" neq "!expectedLine!" (
    set /a diffLnNum=I+1 
    echo Difference detected at line !diffLnNum! ^(actual vrs expected^):
    echo !diffLnNum!. !actualLine!
    echo !diffLnNum!. !expectedLine!
    goto comparison_complete
)
set /a I+=1
goto loop_start_3
:loop_end_3

rem 9. Getting here means file is up to date.
goto end

:comparison_complete

rem 10. Finally write out expected to source file.
set I=0
:loop_start_4
if !I! == !EXPECTED_SIZE! (
    goto loop_end_4
)
set expectedLine=!EXPECTED[%I%]!
rem allow no space between redirection operators and line or else trailing space
rem will be inserted.
if !I! == 0 (
    echo !expectedLine!> "!ACTUAL_VERSION_PATH!"
) else (
    echo !expectedLine!>> "!ACTUAL_VERSION_PATH!"
)
set /a I+=1
goto loop_start_4
:loop_end_4

rem set non-zero exit code to fail build
echo.
echo Source file contents have been successfully updated.
exit /B 1

:end
echo Source file contents are up to date

endlocal