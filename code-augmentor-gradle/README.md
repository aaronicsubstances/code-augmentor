# Gradle Plugin Documentation

## Plugin Specification

```
plugins {
    id 'com.aaronicsubstances.code-augmentor' version '2.0.0'
}
```

## Plugin Tasks

Plugin adds the following tasks to a project:

   * codeAugmentorPrepare
   * codeAugmentorComplete
   * codeAugmentorRun

The first 2 tasks correspond to the preparation and completion stages of Code Augmentor.

The `codeAugmentorRun` tasks combines the functionality of the other two tasks and reduces the needed configuration by working with defaults for some properties. It can only be used with Groovy scripting. Hence if scripting with another language is desired, then the `codeAugmentorRun` task cannot be used; it is `codeAugmentorPrepare` and `codeAugmentorComplete` which will be needed, with use of [Exec](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Exec.html) task or some other means to run code generator scripts.

## Plugin Extension Properties

Plugin adds `codeAugmentor` extension object to a project, which can be used to configure the tasks added to project by this plugin. The properties of the extension are listed below together with their default values, unless otherwise stated that the property is required for its applicable task(s).

   * encoding = "utf-8" (values are validated according to their acceptance by Java's [Charset](https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html) and [StandardCharsets](https://docs.oracle.com/javase/8/docs/api/java/nio/charset/StandardCharsets.html) classes)
   * verbose = true
   * prepFile = "${project.layout.buildDirectory}/codeAugmentor/prepResults.json"
   * genCodeStartDirectives = [ "//:GEN_CODE_START:" ]
   * genCodeEndDirectives = [ "//:GEN_CODE_END:" ]
   * embeddedStringDirectives = [ "//:STR:" ]
   * embeddedJsonDirectives = [ "//:JSON:" ]
   * skipCodeStartDirectives = [ "//:SKIP_CODE_START:" ]
   * skipCodeEndDirectives = [ "//:SKIP_CODE_END:" ]
   * inlineGenCodeDirectives = [ "/\*:GEN_CODE:\*/" ]
   * nestedLevelStartMarkers = [ "{" ]
   * nestedLevelEndMarkers = [ "}" ]
   * augCodeDirectives = [ "//:AUG_CODE:" ]
   * fileSets (required list of [ConfigurableFileTree](https://docs.gradle.org/current/javadoc/org/gradle/api/file/ConfigurableFileTree.html) instances if using `codeAugmentorPrepare` or `codeAugmentorRun` tasks)
   * augCodeSpecs = [ augCodeSpec { destFile = "${project.layout.buildDirectory}/codeAugmentor/augCodes.json"; directives = [ "//:AUG_CODE:" ] } ] *NB: augCodeSpec is a method available on codeAugmentor extension which takes a closure for creating augCodeSpec instances as shown, by setting the two properties* **destFile** *and* **directives**.
   * generatedCodeFiles = [  "${project.layout.buildDirectory}/codeAugmentor/genCodes.json" ]
   * destDir = "${projectLayout.buildDirectory}/codeAugmentor/generated"
   * codeChangeDetectionDisabled = false
   * failOnChanges = true
   * groovyScriptDir (required if using `codeAugmentorRun` task)
   * groovyEntryScriptName = "main.groovy"

## Usage

Following are the properties of each task. Unless otherwise stated, the properties pick values and defaults from properties of the same name as found in the plugin's extension object.

### codeAugmentorPrepare

   * encoding
   * verbose
   * prepFile
   * genCodeStartDirectives
   * genCodeEndDirectives
   * embeddedStringDirectives
   * embeddedJsonDirectives
   * skipCodeStartDirectives
   * skipCodeEndDirectives
   * inlineGenCodeDirectives
   * nestedLevelStartMarkers
   * nestedLevelEndMarkers
   * augCodeSpecs
   * fileSets

### codeAugmentorComplete

   * verbose
   * destDir
   * prepFile
   * generatedCodeFiles
   * codeChangeDetectionDisabled
   * failOnChanges

### codeAugmentorRun
   * encoding
   * verbose
   * genCodeStartDirectives
   * genCodeEndDirectives
   * embeddedStringDirectives
   * embeddedJsonDirectives
   * skipCodeStartDirectives
   * skipCodeEndDirectives
   * inlineGenCodeDirectives
   * nestedLevelStartMarkers
   * nestedLevelEndMarkers
   * augCodeDirectives
   * fileSets
   * groovyScriptDir
   * groovyEntryScriptName
   * destDir
   * codeChangeDetectionDisabled
   * failOnChanges

*NB:*

   - When running `codeAugmentorRun` task, the following properties are hard-coded to their default values:
      - file of parse results of prepare stage (aka prepFile) is set to ${project.layout.buildDirectory}/codeAugmentor/prepResults.json
      - file of augmenting codes produced by prepare stage and meant as input to process stage (aka augCodeFile) is set to ${project.layout.buildDirectory}/codeAugmentor/augCodes.json
      - file of generated codes produced by process stage and meant as input to completion stage (aka genCodeFile) is set to ${project.layout.buildDirectory}/codeAugmentor/genCodes.json

## Example Build Files


### Intended for codeAugmentorRun

`codeAugmentorRun` can easily be hooked to a Gradle build. Sample build file below demonstrates how to make it execute before `compileJava` task, ie before source files are compiled. As mentioned above though, Groovy is the only scripting platform that can be used with this task.

```groovy
plugins {
    id 'java'
    id 'com.aaronicsubstances.code-augmentor'
}
codeAugmentor {
    verbose = true
    fileSets.add(project.fileTree('src/main/java') {
        include '**/*java'
    })
    fileSets.add(project.fileTree('src/test/java') {
        include '**/*java'
    })
    groovyScriptDir = '../codeAugmentorScripts'
}

compileJava.dependsOn codeAugmentorRun
```

**NB:** [Examples](https://github.com/aaronicsubstances/code-augmentor/tree/master/examples) directory contains working gradle plugin demonstration involving this build file.

### Not intended for codeAugmentorRun

This example demonstrates using a scripting platform other than Groovy, such as NodeJS. Gradle still makes it easy for such a setup to be automatically hooked to build, by having entire Code Augmentor operations run before source files are compiled.

```groovy
plugins {
    id 'java'
    id 'com.aaronicsubstances.code-augmentor' version '2.0.0'
}
codeAugmentor {
    verbose = true
    fileSets.add(project.fileTree('src/main/java') {
        include '**/*java'
    })
    fileSets.add(project.fileTree('src/test/java') {
        include '**/*java'
    })
}
task('customProcess', type: Exec) {
    def cmdPlusArgs = []
    /* Uncomment if scripting platform requires cmd.exe to run
     * on Windows OS, such as if executable is .BAT or .CMD
     * file. Python Poetry is an example.
     */ 
    /*if (System.getProperty("os.name") =~ /(?i)windows/) {
        cmdPlusArgs << 'cmd' << '/c'
    }*/
    cmdPlusArgs << 'node'
    cmdPlusArgs << file(codeAugmentor.augCodeSpecs[0].destFile)
    cmdPlusArgs << file(codeAugmentor.generatedCodeFiles[0])
    cmdPlusArgs << "$codeAugmentor.verbose"
    commandLine cmdPlusArgs
    workingDir file('../codeAugmentorScripts')
}

compileJava.dependsOn codeAugmentorComplete
codeAugmentorComplete.dependsOn customProcess
customProcess.dependsOn codeAugmentorPrepare
```
