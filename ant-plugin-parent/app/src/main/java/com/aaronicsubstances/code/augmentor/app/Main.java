package com.aaronicsubstances.code.augmentor.app;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import groovy.lang.GroovyShell;

public class Main {
    private static final String APP_NAME =  "codeaugmentor-app";

    public static void main(String[] args) throws IOException {
        Option fileOpt = Option.builder("f").longOpt("file")
                                .argName( "file" ).hasArg()
                                .desc( "use given buildfile" )
                                .build();
        Option helpOpt = Option.builder("h").longOpt("help")
                                .desc("help information")
                                .build();
        Options options = new Options();
        options.addOption(fileOpt).addOption(helpOpt);
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse( options, args);
        }
        catch (ParseException ex) {
            System.err.println(ex.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(APP_NAME, options );
            System.exit(1);
            return;
        }
        if (cmd.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(APP_NAME, options );
            return;
        }
        String filePath = cmd.getOptionValue('f');
        if (filePath == null) {
            filePath = "build.groovy";
        }
        GroovyShell groovyShell = new GroovyShell();
        File file = new File(filePath);
        groovyShell.run(file, args);
    }
}