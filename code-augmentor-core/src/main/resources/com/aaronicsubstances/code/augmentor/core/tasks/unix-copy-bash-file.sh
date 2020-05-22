#!/usr/bin/env sh

if [ "$1" == "-f" ]; then confirmed=1; fi
DIRNAME=`dirname $0`
if [ -z "$DIRNAME" ]; then DIRNAME=.; fi
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
        else
            echo "$counter. will copy $c/$a to $b/$a"
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
echo
if [ -n "$confirmed" ]; then
    echo "Done"
else
    echo "NB: Add -f as first command line arg to actually copy and overwrite"
fi