# Ant Plugin Documentation

The available ant tasks are

   - code_aug_prepare
   - code_aug_complete
   - code_aug_run

To load tasks, use

```xml
<taskdef resource="CodeAugmentorTasks.xml" onerror="failall" />
```

The first two tasks correspond to the preparation and completion stages of Code Augmentor. Use of [exec](https://ant.apache.org/manual/Tasks/exec.html) task or some other means is expected to carry out the intervening processing stage.

The last `run` task is relevant only in a context where Code Augmentor is embedded as a Java library. In that case `run` task enables one to specify a library routine that is used to process augmenting codes, and avoid having to create an OS process first. Intended use cases include embedding Code Augmentor into a JVM application such as through Groovy's [AntBuilder](http://docs.groovy-lang.org/latest/html/gapi/groovy/ant/AntBuilder.html); and integrating with non-JVM languages through [GraalVM](https://www.graalvm.org/).

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

### code_aug_run

```xml
<code_aug_run
        encoding="utf-8"
        verbose="false"
        prepFile="${ant.basedir}/build/codeAugmentor/parseResults.json"
        augCodeFile="${ant.basedir}/build/codeAugmentor/augCodes.json"
        genCodeFile="${ant.basedir}/build/codeAugmentor/genCodes.json"
        stackTraceLimitPrefixes="groovy.util.GroovyScriptEngine"
        stackTraceFilterPrefixes="com.sun., sun., groovy.lang., org.codehaus.groovy."
        destDir="${ant.basedir}/build/codeAugmentor/generated"
        codeChangeDetectionDisabled="false"
        failOnChanges="true">
    <augCodeDirective value="//:AUG_CODE:" />
    <genCodeStartDirective value="//:GEN_CODE_START:" />
    <genCodeEndDirective value="//:GEN_CODE_END:" />
    <embeddedStringDirective value="//:STR:" />
    <embeddedJsonDirective value="//:JSON:" />
    <skipCodeStartDirective value="//:SKIP_CODE_START:" />
    <skipCodeEndDirective value="//:SKIP_CODE_END:" />
    <inlineGenCodeDirective value="/*:GEN_CODE:*/" />
    <nestedLevelStartMarker value="{" />
    <nestedLevelEndMarker value="}" />
    <srcDir dir="src/main/java" includes="**/*.java" />
    <srcDir dir="src/test/java" includes="**/*.java" />
</code_aug_run>
```

*NB:*
   
   - **stackTraceLimitPrefixes** are comma separated strings or numbers which limit the size of stack traces to show when exceptions are encounterd during processing stage. Preference is given first to the strings which are prefixes of classes which if encountered in stack trace, should mark the end of the stack trace display. If stack trace doesn't contain any of the strings and numbers are present in stackTraceLimitPrefixes, then the numbers will be used to determine how many stack trace elements to display for each nested exception encountered.
   - **stackTraceFilterPrefixes** are comman separated strings which are prefixes of classes which should cause stack trace element to be skipped if encountered.
   - Both stack trace related properties help to limit the stack trace noise displayed during exceptions when using dynamic JVM languages like Groovy, so that the relevant error messages stand out.
   - The following references have to be added to the [project](https://ant.apache.org/manual/api/org/apache/tools/ant/Project.html) property of this Ant plugin task of type com.aaronicsubstances.code.augmentor.ant.CodeAugmentorTask:
       
       - **codeAugmentor.jsonParseFunction** - instance of com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask.JsonParseFunction interface used to parse JSON data in augmenting code sections.

       - **codeAugmentor.scriptEvalFunction** - instance of com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask.EvalFunction interface which serves as the routine for processing augmenting codes into generated codes.

       - *codeAugmentor.defaultStackTraceLimitPrefixes* - optional List<String> which will be used instead of default of [ groovy.util.GroovyScriptEngine ]


## Sample Build File

The following Ant build file demonstrates expected usage of the plugin in which `code_aug_prepare` and `code_aug_complete` tasks are used, and code_aug_run task is ignored.

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
