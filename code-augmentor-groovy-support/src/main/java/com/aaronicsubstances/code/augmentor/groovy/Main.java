package com.aaronicsubstances.code.augmentor.groovy;

import java.io.File;
import java.net.URL;
import java.util.Arrays;

import com.aaronicsubstances.code.augmentor.ant.CodeAugmentorTask;
import com.aaronicsubstances.code.augmentor.ant.CompletionTask;
import com.aaronicsubstances.code.augmentor.ant.PrepareTask;
import com.aaronicsubstances.code.augmentor.ant.ProcessTask;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeContext;
import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask;

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
import groovy.json.JsonSlurper;
import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;

public class Main {

    public static void main(String[] args) throws Exception {
        String APP_NAME = System.getProperty("app.name");
        if (APP_NAME == null) {
            APP_NAME =  "codeaugmentor-groovy-app";
        }
        Option dirOpt = Option.builder("d").longOpt("dir").required()
                                .argName( "groovy dir" ).hasArg()
                                .desc( "use given Groovy script directory" )
                                .build();
        Option helpOpt = Option.builder("h").longOpt("help")
                                .desc("help information")
                                .build();
        Options options = new Options();
        options.addOption(dirOpt)
            .addOption(helpOpt);

        if (hasHelp(helpOpt, args)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(APP_NAME, options, true);
            return;
        }

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args, true);
        }
        catch (ParseException ex) {
            System.err.println(ex.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(APP_NAME, options, true);
            System.exit(1);
            return;
        }
        String scriptDirPath = cmd.getOptionValue(dirOpt.getOpt());
        startGroovyScript(args, new File(scriptDirPath));
    }
    
    private static boolean hasHelp(Option help, String[] args) {
        Options options = new Options();
        options.addOption(help);
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.getOptions().length != 1) {
                return false;
            }
            return cmd.hasOption(help.getOpt());
        }
        catch (ParseException ignore) {
            return false;
        }
    }

    private static void startGroovyScript(String[] args, File scriptDir) throws Exception {
        URL[] scriptEngineRoots = new URL[]{ scriptDir.toURI().toURL() };
        GroovyScriptEngine scriptEngine = new GroovyScriptEngine(scriptEngineRoots);
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.setRecompileGroovySource(false);
        scriptEngine.setConfig(cc);
        Binding binding = new Binding();
        binding.setVariable("args", args);
        Script entryScript = scriptEngine.createScript("main.groovy", binding);
        AntBuilder antBuilder = createAntBuilder(scriptDir, entryScript);
        binding.setVariable("ant", antBuilder);
        entryScript.run();
    }

    private static AntBuilder createAntBuilder(File scriptDir, Script entryScript) {
        AntBuilder antBuilder = new AntBuilder();
        Project antProject = antBuilder.getProject();
        antProject.setBaseDir(scriptDir);
        setUpTaskDefinitions(antProject);

        antProject.addReference(ProcessTask.PROJECT_REFERENCE_DEFAULT_STACK_TRACE_LIMIT_PREFIXES, 
            Arrays.asList(Main.class.getName()));

        antProject.addReference(ProcessTask.PROJECT_REFERENCE_JSON_PARSE_FUNCTION,
            new ProcessCodeGenericTask.JsonParseFunction(){
                final JsonSlurper jsonParser = new JsonSlurper();
                @Override
                public Object parse(String json) throws Exception {                    
                    return jsonParser.parseText(json);
                }
            }
        );

        antProject.addReference(ProcessTask.PROJECT_REFERENCE_SCRIPT_EVAL_FUNCTION, 
            new ProcessCodeGenericTask.EvalFunction() {
                @Override
                public Object apply(String functionName, AugmentingCode augCode, 
                        ProcessCodeContext context) {
                    final String disambiguatingPrefix = "codeAugmentorVariable_";
                    String augCodeVarName = disambiguatingPrefix + "augCode";
                    String contextVarName = disambiguatingPrefix + "context";
                    entryScript.getBinding().setVariable(augCodeVarName, augCode);
                    entryScript.getBinding().setVariable(contextVarName, context);
                    return entryScript.evaluate(functionName + "(" +
                        augCodeVarName + ", " + contextVarName + ")");
                }
            }
        );

        return antBuilder;
    }

    private static void setUpTaskDefinitions(Project antProject) {
        antProject.addTaskDefinition("code_aug_run", CodeAugmentorTask.class);
        antProject.addTaskDefinition("code_aug_prepare", PrepareTask.class);
        antProject.addTaskDefinition("code_aug_process", ProcessTask.class);
        antProject.addTaskDefinition("code_aug_complete", CompletionTask.class);
    }
}