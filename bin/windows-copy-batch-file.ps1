$CMDNAME = [System.IO.Path]::GetFileName($PSCommandPath)
function printHelp {
    echo ""
    echo "Usage: $CMDNAME [-f | -h | [--rev] <arbitraryCommandWithArgs>]"
    echo "where"
    echo "    -f                              use to actually copy and overwrite"
    echo ""
    echo "    -h                              prints this help information and exits"
    echo ""
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
    echo "      $CMDNAME fc /n /t"
    echo '   to use built-in File Compare program on Windows to view file differences'
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

$arbitraryCommand = "$args"
if ($args[0] -eq "--rev")
{
    $reverseFileNames = 1
    $arbitraryCommand = "$args[1..-1]"
}
if ($arbitraryCommand -eq "-f" -and !$reverseFileNames)
{
    $confirmed = 1
}
if ($arbitraryCommand -eq "-h" -and !$reverseFileNames)
{
    PrintHelp
    exit
}
$counter = 0
foreach ($line in $input)
{
    if ($a)
    {
        $b = $line
        $counter = $counter + 1
        if ($confirmed)
        {
            Write-Output "$counter. copying $b to $a"
            Copy-Item -Path "$b" -Destination "$a" -Force -errorAction stop
        }
        elseif ($arbitraryCommand)
        {
            if ($reverseFileNames)
            {
                $eventualCmd = "$arbitraryCommand ""$b"" ""$a"""
            }
            else
            {
                $eventualCmd = "$arbitraryCommand ""$a"" ""$b"""
            }
            if ($IsLinux -or $IsMacOS)
            {
                sh $eventualCmd
            }
            else
            {
                CMD /c $eventualCmd
            }
            # could not add this because diff programs set nonzero error statuses
            # upon encountering a difference.
            #if ($lastexitcode)
            #{
            #    exit $lastexitcode
            #}
        }
        else
        {
            echo "$counter. $b"
        }
        $a = ""
    }
    else
    {
        $a = $line
    }
}

if ($confirmed)
{
    echo ""
    echo "Done"
}
elseif (!$arbitraryCommand)
{
    PrintHelp
}
