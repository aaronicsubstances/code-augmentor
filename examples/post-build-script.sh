#!/usr/bin/env sh

# 1. Specify the command to execute and get the latest version.
EXPECTED_VERSION_COMMAND="git rev-parse master"

# 2. Receive path to source file for recording latest version as first 
#    command line argument.
# NB: empty lines will be ignored.
ACTUAL_VERSION_PATH=$1
if [ -f "$ACTUAL_VERSION_PATH" ]; then 
    # ok, file exists.
    : # empty body
else
    if [ -z "$ACTUAL_VERSION_PATH" ]; then
        echo "Path to source file for recording latest version is required as first command line argument" 1>&2
    else
        echo "Provided path to source file for recording latest version does not exist: $ACTUAL_VERSION_PATH" 1>&2
    fi
    exit 1
fi

# 3. Add non empty header lines which precede latest version information.
EXPECTED_SIZE=0
declare EXPECTED_$EXPECTED_SIZE="H"
EXPECTED_SIZE=`expr $EXPECTED_SIZE + 1`

# 4. Now execute command and get lines of STDOUT/STDERR as latest version information.
# NB: Getting STDERR and ignoring exit status makes script resemble windows batch version.
# NB: Observed that java -version and javac -version write to STDERR only.
cmdOutput=`eval $EXPECTED_VERSION_COMMAND 2>&1`
while IFS= read -r line
do
    # skip empty lines to match similar behaviour in windows batch script counterpart.
    if [ -n "$line" ]
    then
        declare EXPECTED_$EXPECTED_SIZE="$line"
        EXPECTED_SIZE=`expr $EXPECTED_SIZE + 1`
    fi
done <<< "$cmdOutput"

# 5. Add non empty trailing lines which follow latest version information.
declare EXPECTED_$EXPECTED_SIZE="T"
EXPECTED_SIZE=`expr $EXPECTED_SIZE + 1`

# 6. Confirm all lines have been gathered correctly.
echo "Expected lines from \"$EXPECTED_VERSION_COMMAND\":"
I=0
while [ $I -lt $EXPECTED_SIZE ]
do
    diffLnNum=`expr $I + 1` 
    itemName=EXPECTED_$I
    eval itemValue=\$$itemName
    echo "$diffLnNum. $itemValue"
    I=`expr $I + 1`
done

# 7. Now gather the actual version file contents.
#    NB: convert CRLFs so that files updated by windows scripts
#        remain up to date with this script as well.
fileInput=`dos2unix < "$ACTUAL_VERSION_PATH"`
ACTUAL_SIZE=0
while IFS= read -r line
do
    # skip empty lines to match similar behaviour in windows batch script counterpart.
    if [ -n "$line" ]
    then
        declare ACTUAL_$ACTUAL_SIZE="$line"
        ACTUAL_SIZE=`expr $ACTUAL_SIZE + 1`
    fi
done <<< "$fileInput"

echo
echo "Actual lines read from \"$ACTUAL_VERSION_PATH\":"
I=0
while [ $I -lt $ACTUAL_SIZE ]
do
    diffLnNum=`expr $I + 1` 
    itemName=ACTUAL_$I
    eval itemValue=\$$itemName
    echo "$diffLnNum. $itemValue"
    I=`expr $I + 1`
done

# 8. Now compare actual file contents against expected for equality.
echo
upToDate=1
if [ $ACTUAL_SIZE == $EXPECTED_SIZE ]; then
    I=0
    while [ $I -lt $ACTUAL_SIZE ]
    do 
        itemName=ACTUAL_$I
        eval actualLine=\$$itemName
        itemName=EXPECTED_$I
        eval expectedLine=\$$itemName
        if [ "$actualLine" != "$expectedLine" ]; then
            diffLnNum=`expr $I + 1`
            echo "Difference detected at line $diffLnNum (actual vrs expected):"
            echo "$diffLnNum. $actualLine"
            echo "$diffLnNum. $expectedLine"
            upToDate=
            break
        fi
        I=`expr $I + 1`
    done
else
    echo "Difference detected in line counts (actual vrs expected): $ACTUAL_SIZE neq $EXPECTED_SIZE"
    upToDate=
fi

# 9. exit successfully without touch source file if contents match expected.
if [ -n "$upToDate" ]; then
    echo "Source file contents are up to date"
    exit
fi

# 10. Finally write out expected to source file.
I=0
while [ $I -lt $EXPECTED_SIZE ]
do
    diffLnNum=`expr $I + 1` 
    itemName=EXPECTED_$I
    eval itemValue=\$$itemName
    if [ $I == 0 ]; then
        echo "$itemValue" > "$ACTUAL_VERSION_PATH"
    else
        echo "$itemValue" >> "$ACTUAL_VERSION_PATH"
    fi
    I=`expr $I + 1`
done

# set non-zero exit code to fail build
echo
echo "Source file contents have been successfully updated."
exit 1