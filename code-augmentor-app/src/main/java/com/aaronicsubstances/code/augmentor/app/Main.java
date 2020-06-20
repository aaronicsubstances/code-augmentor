package com.aaronicsubstances.code.augmentor.app;

import java.io.File;

import com.aaronicsubstances.code.augmentor.ant.CompletionTask;
import com.aaronicsubstances.code.augmentor.ant.PrepareTask;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.NoBannerLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

public class Main {

    public static void main(String[] args) throws Exception {
        String APP_NAME = System.getProperty("app.name");
        String APP_VERSION = System.getProperty("app.version");
        if (APP_NAME == null) {
            APP_NAME = "codeaugmentorapp";
        }
        Option fileOpt = Option.builder("f").longOpt("file").required()
                                .argName( "ant build file" ).hasArg()
                                .desc( "use given Ant build XML file" )
                                .build();
        Option antTargetOpt = Option.builder("t").longOpt("target")
                                .argName( "ant target" ).hasArg()
                                .desc( "use given Ant build target" )
                                .build();
        Option helpOpt = Option.builder("h").longOpt("help")
                                .desc("print help information and exit")
                                .build();
        Option versionOpt = Option.builder("v").longOpt("version")
                                .desc("print version and exit")
                                .build();
        Options options = new Options();
        options.addOption(fileOpt)
            .addOption(antTargetOpt)
            .addOption(helpOpt)
            .addOption(versionOpt);

        if (hasSingleOption(helpOpt, args)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(APP_NAME, options, true);
            return;
        }

        if (hasSingleOption(versionOpt, args)) {
            System.out.format("%s %s%n", APP_NAME, APP_VERSION);
            return;
        }

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        }
        catch (ParseException ex) {
            System.err.println(ex.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(APP_NAME, options, true);
            System.exit(1);
            return;
        }
        String buildPath = cmd.getOptionValue(fileOpt.getOpt());
        startAntBuild(new File(buildPath), cmd.getOptionValue(antTargetOpt.getOpt()));
    }
    
    private static boolean hasSingleOption(Option singleOpt, String[] args) {
        Options options = new Options();
        options.addOption(singleOpt);
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.getOptions().length != 1) {
                return false;
            }
            return cmd.hasOption(singleOpt.getOpt());
        }
        catch (ParseException ignore) {
            return false;
        }
    }

    private static void startAntBuild(File buildFile, String target) {
        Project antProject = new Project();
        ProjectHelper helper = ProjectHelper.getProjectHelper();
        antProject.addReference(ProjectHelper.PROJECTHELPER_REFERENCE, helper);
        final BuildLogger logger = new NoBannerLogger();
        logger.setMessageOutputLevel(Project.MSG_INFO);
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);
        antProject.addBuildListener(logger);
        antProject.init();
        setUpTaskDefinitions(antProject);
        helper.parse(antProject, buildFile);
        if (target == null || target.isEmpty()) {
            target = antProject.getDefaultTarget();
        }
        antProject.executeTarget(target);
    }

    private static void setUpTaskDefinitions(Project antProject) {
        antProject.addTaskDefinition("code_aug_prepare", PrepareTask.class);
        antProject.addTaskDefinition("code_aug_complete", CompletionTask.class);
    }
}