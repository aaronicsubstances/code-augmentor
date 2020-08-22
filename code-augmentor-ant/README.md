# Ant Plugin Documentation

## Plugin Specification

**NB:** *If running the ant plugin from code-augmentor-app, then the ant classpath and tasks are already setup, and this step can be skipped.*

The ant classpath needs to be manually set up with jars from Maven Central with the following coordinates:

   * com.aaronicsubstances:code-augmentor-core:2.1.0
   * com.aaronicsubstances:code-augmentor-ant:2.1.0
   * com.google.code.gson:gson:2.8.6
 
Then to load the tasks, use

```xml
<taskdef resource="CodeAugmentorTasks.xml" onerror="failall" />
```

## Plugin Tasks

The available ant tasks are

   - code_aug_prepare
   - code_aug_complete

The two tasks correspond to the preparation and completion stages of Code Augmentor. Use of [exec](https://ant.apache.org/manual/Tasks/exec.html) task or some other means is expected to carry out the intervening processing stage.

## Usage

Following are the fields and nested elements of each task. Unless otherwise stated, fields and nested elements are optional, with default values specified in each sample task specification.

### code_aug_prepare

```xml
<code_aug_prepare
        encoding="utf-8"
        verbose="false"
        prepFile="${ant.basedir}/build/codeAugmentor/parseResults.json">
    <genCodeStartDirective value="//:GEN_CODE_START:" />
    <genCodeEndDirective value="//:GEN_CODE_END:" />
    <embeddedStringDirective value="//:STR:" />
    <embeddedJsonDirective value="//:JSON:" />
    <skipCodeStartDirective value="//:SKIP_CODE_START:" />
    <skipCodeEndDirective value="//:SKIP_CODE_END:" />
    <inlineGenCodeDirective value="/*:GEN_CODE:*/" />
    <nestedLevelStartMarker value="{" />
    <nestedLevelEndMarker value="}" />
    <augCodeSpec 
            destFile="${ant.basedir}/build/codeAugmentor/augCodes.json">
        <directive value="//:AUG_CODE:" />
    </augCodeSpec>
    <srcDir dir="src/main/java" includes="**/*.java" />
    <srcDir dir="src/test/java" includes="**/*.java" />
</code_aug_prepare>
```

*NB:*

   - At least one nested **srcDir** element of type [FileSet](https://ant.apache.org/manual/Types/fileset.html) is required.
   - Values for **encoding** attribute are validated according to their acceptance by Java's [Charset](https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html) and [StandardCharsets](https://docs.oracle.com/javase/8/docs/api/java/nio/charset/StandardCharsets.html) classes.
   - Each nested element (**genCodeStartDirective**, **genCodeEndDirective**, **embeddedStringDirective**, **embeddedJsonDirective**, **skipCodeStartDirective**, **skipCodeEndDirective**, **inlineGenCodeDirective**, **nestedLevelStartMarker**, **nestedLevelEndMarker**, **augCodeSpec**, **directive**, **srcDir**) can be specified more than once.

### code_aug_complete

```xml
<code_aug_complete
        verbose="false"
        destDir="${ant.basedir}/build/codeAugmentor/generated"
        prepFile="${ant.basedir}/build/codeAugmentor/parseResults.json"
        codeChangeDetectionDisabled="false"
        failOnChanges="true">
    <genCodeSpec 
        file="${ant.basedir}/build/codeAugmentor/genCodes.json" />
</code_aug_complete>
```

*NB:*

   - Nested **genCodeSpec** element can be specified more than once.


## Sample Build File

```xml
<project name="sample" default="default">
    <description>Sample build file for code-augmentor-app</description>
    
    <taskdef resource="CodeAugmentorTasks.xml" onerror="failall" />

    <property name="src.dir" location="src" />
    <property name="build.dir" location="build" />
    <property name="build.code-augmentor.dir" location="${build.dir}/codeAugmentor" />
    <property name="code-augmentor.verbose" value="true" />
    <property name="augCode.file" location="${build.code-augmentor.dir}/augCodes.json" />
    <property name="genCode.file" location="${build.code-augmentor.dir}/genCodes.json" />

    <property name="script.executable.path" value="node" /> <!-- or php, python3, python, groovy --> 
    <property name="script.main.file" value="main.js" /> <!-- or main.php, main.py, main.groovy -->
    
    <target name="default" description="runs entire code augmentor operation with default settings">
        <delete dir="${build.dir}" />
        
        <code_aug_prepare verbose="${code-augmentor.verbose}">
            <srcDir dir="${src.dir}">
                <include name="**/*.java" />
            </srcDir>
        </code_aug_prepare>
    
        <exec executable="${script.executable.path}" failonerror="true" vmlauncher="false">
            <arg value="${script.main.file}" />
            <arg value="${augCode.file}" />
            <arg value="${genCode.file}" />
            <arg value="${code-augmentor.verbose}" />
        </exec>
    
        <code_aug_complete verbose="${code-augmentor.verbose}" />
    </target>
    
</project>
```
