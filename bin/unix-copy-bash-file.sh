#!/usr/bin/env sh

CMDNAME=`basename $0`

printHelp() {
    echo
    echo "Usage: $CMDNAME [-f | -h | [--rev] <arbitraryCommandWithArgs>]"
    echo "where"
    echo "    -f                              use to actually copy and overwrite"
    echo
    echo "    -h                              prints this help information and exits"
    echo
    echo ""
    echo '    <arbitraryCommandWithArgs>      use to specify a command which will be called iteratively'
    echo "                                    with each source file and its corresponding generated file" 
    echo "                                    appended to its args. Can prepend command with --rev to"
    echo "                                    rather have generated file appended before corresponding"
    echo "                                    source file. Exit value of command invocation in each iteration"
    echo "                                    is NOT checked."
    echo ""
    echo "NB: If no arguments are specified, only generated files are printed and then "
    echo "this help message is printed afterwards."
    echo ""
    echo "EXAMPLES"
    echo ""
    echo "1. Use"
    echo "      $CMDNAME -f"
    echo '   to overwrite each source file with its corresponding generated file.'
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
    echo "      $CMDNAME git diff --no-index"
    echo "   to view file differences with Git."
    echo ""
    echo '5. Can use'
    echo "      $CMDNAME --rev cp -f -v"
    echo "   to achieve similar effect as $CMDNAME -f (but without error checking)"
    echo ""
}

if [ "$1" == "--rev" ]; then
    reverseFileNames=1
    shift
fi

arbitraryCommand="$@"

if [ "$arbitraryCommand" == "-h" ] && [ -z "$reverseFileNames" ]; then
    printHelp
    exit
fi

if [ "$arbitraryCommand" == "-f" ] && [ -z "$reverseFileNames" ]; then
    confirmed=1
fi

if [ -n "$confirmed" ]; then
    
elif [ -z "$arbitraryCommand" ]; then
    printHelp
fi

while IFS= read -r line
do
    if [ -n "$a" ]
    then
        b="$line"
        b="${b//$'\r'/}" # remove any carriage return.
        counter=`expr $counter + 1`
        if [ -n "$confirmed" ]
        then
            echo "$counter. copying $b to $a"
            cp -f "$b" "$a"
            if [ $? -ne 0 ]; then
                exit $?
            fi
        elif [ -n "$arbitraryCommand" ]
        then
            if [ -n "$reverseFileNames" ]; then
                $arbitraryCommand "$b" "$a"
            else
                $arbitraryCommand "$a" "$b"
            fi
            # could not add this because diff programs set nonzero error statuses
            # upon encountering a difference.
            # if [ $? -ne 0 ]; then
                # exit $?
            # fi
        else
            echo "$counter. $b"
        fi
        a=
    else  
        a="$line"
        a="${a//$'\r'/}" # remove any carriage return.
    fi
done < "/dev/stdin"

if [ -n "$confirmed" ]; then
    echo
    echo "Done"
fi
