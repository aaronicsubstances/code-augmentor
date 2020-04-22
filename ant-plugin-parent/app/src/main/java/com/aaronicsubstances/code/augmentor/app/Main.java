package com.aaronicsubstances.code.augmentor.app;

import java.io.File;
import java.net.URL;

import com.aaronicsubstances.code.augmentor.ant.CodeAugmentorTask;
import com.aaronicsubstances.code.augmentor.ant.CompletionTask;
import com.aaronicsubstances.code.augmentor.ant.PrepareTask;
import com.aaronicsubstances.code.augmentor.ant.ProcessTask;
import com.aaronicsubstances.code.augmentor.core.tasks.GenericTaskExtensionFunction;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.tools.ant.Project;
import org.codehaus.groovy.control.CompilerConfiguration;

import groovy.ant.AntBuilder;
import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;

public class Main {
    private static final String APP_NAME =  "codeaugmentor-app";

    public static void main(String[] args) throws Exception {
        Option fileOpt = Option.builder("d").longOpt("dir")
                                .required()
                                .argName( "dir" ).hasArg()
                                .desc( "use given directory" )
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
        String scriptDirPath = cmd.getOptionValue('d');
        File scriptDir = new File(scriptDirPath);
        URL[] scriptEngineRoots = new URL[]{ scriptDir.toURI().toURL() };
        GroovyScriptEngine scriptEngine = new GroovyScriptEngine(scriptEngineRoots);
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setRecompileGroovySource(false);
        scriptEngine.setConfig(cc);
        Binding binding = new Binding();
        binding.setVariable("args", cmd);
        Script entryScript = scriptEngine.createScript("main.groovy", binding);
        AntBuilder antBuilder = createAntBuilder(scriptDir, entryScript);
        binding.setVariable("ant", antBuilder);
        binding.setVariable("defaultStackTraceLimitPrefixes", Main.class.getName());
        entryScript.run();
    }

    private static AntBuilder createAntBuilder(File scriptDir, Script entryScript) {
        AntBuilder antBuilder = new AntBuilder();
        Project antProject = antBuilder.getProject();
        antProject.setBaseDir(scriptDir);
        antProject.addTaskDefinition("code_aug_run", CodeAugmentorTask.class);
        antProject.addTaskDefinition("code_aug_prepare", PrepareTask.class);
        antProject.addTaskDefinition("code_aug_process", ProcessTask.class);
        antProject.addTaskDefinition("code_aug_complete", CompletionTask.class);

        antProject.addReference("scriptEvalFunction", new GenericTaskExtensionFunction() {
            @Override
            public Object makeFunctionCall(Object[] args) {
                String functionName = (String) args[0];
                String disambiguatingPrefix = "SCRIPT_EVAL_FUNCTION_ARG_";
                String augCodeVarName = disambiguatingPrefix + "augCode";
                String contextVarName = disambiguatingPrefix + "context";
                entryScript.getBinding().setVariable(augCodeVarName, args[1]);
                entryScript.getBinding().setVariable(contextVarName, args[2]);
                return entryScript.evaluate(functionName + "(" +
                    augCodeVarName + ", " + contextVarName + ")");
            }
        });

        return antBuilder;
    }
}