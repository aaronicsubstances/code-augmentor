#!/usr/bin/env sh

if [ "$1" == "-f" ]; then confirmed=1; fi
DIRNAME=`dirname $0`
if [ -z "$DIRNAME" ]; then DIRNAME=.; fi

CMDNAME=`basename $0`

if [ "$1" == "--rev" ]; then
    reverseFileNames=1
    shift
fi

arbitraryCommand="$@"
if [ "$arbitraryCommand" == "-f" ]; then
    set confirmed=1
    arbitraryCommand=
fi

while IFS= read -r line
do
    if [ -n "$b" ]
    then
        c="$line"
        counter=`expr $counter + 1`
        if [ -n "$confirmed" ]
        then
            echo "$counter. copying $c/$a to $b/$a"
            cp -f "$c/$a" "$b/$a"
            if [ $? -ne 0 ]; then
                exit $?
            fi
        elif [ -n "$arbitraryCommand" ]
        then
            if [ -n "$reverseFileNames" ]; then
                $arbitraryCommand "$c/$a" "$b/$a"
            else
                $arbitraryCommand "$b/$a" "$c/$a"
            fi
        else
            echo "$counter. $a"
        fi
        a=
        b=
    else  
        if [ -n "$a" ]
        then
            b="$line"
        else
            a="$line"
        fi
    fi
done < "$DIRNAME/CHANGE-SUMMARY.txt"
if [ -n "$confirmed" ]; then
    echo
    echo "Done"
elif [ -z "$arbitraryCommand" ]; then
    echo
    echo "Usage: $CMDNAME [-f | [--rev] <arbitraryCommandWithArgs>]"
    echo "where"
    echo "    -f                              use to actually copy and overwrite"
    echo ""
    echo '    <arbitraryCommandWithArgs>      use to specify a command which will be called iteratively'
    echo "                                    with each source file and its corresponding generated file" 
    echo "                                    appended to its args. Can prepend command with --rev to"
    echo "                                    rather have generated file appended before corresponding"
    echo "                                    source file. Exit value of command invocation in each iteration"
    echo "                                    is NOT checked."
    echo ""
    echo "NB: If no arguments are specified, only relative paths of source files are printed and then "
    echo "this help message is printed afterwards."
    echo ""
    echo "EXAMPLES"
    echo ""
    echo "1. Use"
    echo "      $CMDNAME -f"
    echo '   to overwrite each source file with its corresponding generated file to achieve synchronization'
    echo '   goal of Code Augmentor.'
    echo ""
    echo '2. Can use'
    echo "      $CMDNAME echo"
    echo '   to view full paths of each changed source file and its corresponding generated file on a line.'
    echo ""
    echo '3. Can use'
    echo "      $CMDNAME diff"
    echo '   to use built-in diff program on Unix/Linux to view file differences'
    echo "" 
    echo '4. Can use'
    echo "      $CMDNAME git diff"
    echo "   to view file differences with Git."
    echo "" 
    echo '5. Can use'
    echo "      $CMDNAME git --no-pager diff > diff.txt"
    echo '   to save file differences with Git.'
    echo ""
    echo '6. Can use'
    echo "      $CMDNAME --rev cp -f -v"
    echo "   to achieve similar effect as $CMDNAME -f (but without error checking)"
    echo ""
fi