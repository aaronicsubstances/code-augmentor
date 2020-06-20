# Maven Plugin Documentation

## Plugin specification

```xml
<plugin>
    <groupId>com.aaronicsubstances</groupId>
    <artifactId>code-augmentor-maven-plugin</artifactId>
    <version>2.0.0</version>
</plugin>
```

## Goals

The plugin provides the following goals:

   * code-augmentor:prepare
   * code-augmentor:complete
   * code-augmentor:run

The first 2 goals correspond to the preparation and completion stages of Code Augmentor.

The `run` goal combines the functionality of the other two goals and reduces the needed configuration by working with defaults for some properties. It can only be used with Groovy scripting. Hence if scripting with another language is desired, then the `run` goal cannot be used; it is `prepare` and `complete` which will be needed, with use of [exec-maven-plugin](https://www.mojohaus.org/exec-maven-plugin/) or some other means to run code generator scripts.

## Usage

Following are the configuration of each goal. Unless otherwise stated, configuration properties are optional, with default values specified as the configuration value.

### code-augmentor:prepare

```xml
<configuration>
    <encoding>${project.build.sourceEncoding}</encoding>
    <verbose>false</verbose>
    <prepFile>${project.build.directory}/codeAugmentor/prepResults.json</prepFile>
    <genCodeStartDirectives>
        <directive>//:GEN_CODE_START:</directive>
    </genCodeStartDirectives>
    <genCodeEndDirectives>
        <directive>//:GEN_CODE_END:</directive>
    </genCodeEndDirectives>
    <embeddedStringDirectives>
        <directive>//:STR:</directive>
    </embeddedStringDirectives>
    <embeddedJsonDirectives>
        <directive>//:JSON:</directive>
    </embeddedJsonDirectives>
    <skipCodeStartDirectives>
        <directive>//:SKIP_CODE_START:</directive>
    </skipCodeStartDirectives>
    <skipCodeEndDirectives>
        <directive>//:SKIP_CODE_END:</directive>
    </skipCodeEndDirectives>
    <inlineGenCodeDirectives>
        <directive>/*:GEN_CODE:*/</directive>
    </inlineGenCodeDirectives>
    <nestedLevelStartMarkers>
        <marker>{</marker>
    </nestedLevelStartMarkers>
    <nestedLevelEndMarkers>
        <marker>}</marker>
    </nestedLevelEndMarkers>
    <augCodeSpecs>
        <spec>
            <destFile>${project.build.directory}/codeAugmentor/augCodes.json</destFile>
            <directives>
                <directive>//:AUG_CODE:</directive>
            </directives>
        </spec>
    </augCodeSpecs>
    <fileSets>
        <fileSet>
            <directory>src/main/java</directory>
            <includes>
              <include>**/*.java</include>
            </includes>
        </fileSet>
        <fileSet>
            <directory>src/test/java</directory>
            <includes>
              <include>**/*.java</include>
            </includes>
        </fileSet>
    </fileSets>
</configuration>
```


*NB:*
   - At least one **fileSets** child element of type [FileSet](https://maven.apache.org/shared/file-management/fileset.html) is required.
   - Values for **encoding** attribute are validated according to their acceptance by Java's [Charset](https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html) and [StandardCharsets](https://docs.oracle.com/javase/8/docs/api/java/nio/charset/StandardCharsets.html) classes. If project.build.sourceEncoding property is not set, UTF-8 will be used as default.
   - These child elements can be specified more than once: **directive**, **marker**, **spec**, **fileSet** 


### code-augmentor:complete

```xml
<configuration>
    <verbose>false</verbose>
    <destDir>${project.build.directory}/codeAugmentor/generated</destDir>
    <prepFile>${project.build.directory}/codeAugmentor/prepResults.json</prepFile>
    <codeChangeDetectionDisabled>false</codeChangeDetectionDisabled>
    <failOnChanges>true</failOnChanges>
    <generatedCodeFiles>
        <genCodeFile>${project.build.directory}/codeAugmentor/genCodes.json</genCodeFile>
    </generatedCodeFiles>
</configuration>
```

*NB:*
   - These child elements can be specified more than once: **genCodeFile**

### code-augmentor:run

```xml
<configuration>
    <encoding>${project.build.sourceEncoding}</encoding>
    <verbose>false</verbose>
    <genCodeStartDirectives>
        <directive>//:GEN_CODE_START:</directive>
    </genCodeStartDirectives>
    <genCodeEndDirectives>
        <directive>//:GEN_CODE_END:</directive>
    </genCodeEndDirectives>
    <embeddedStringDirectives>
        <directive>//:STR:</directive>
    </embeddedStringDirectives>
    <embeddedJsonDirectives>
        <directive>//:JSON:</directive>
    </embeddedJsonDirectives>
    <skipCodeStartDirectives>
        <directive>//:SKIP_CODE_START:</directive>
    </skipCodeStartDirectives>
    <skipCodeEndDirectives>
        <directive>//:SKIP_CODE_END:</directive>
    </skipCodeEndDirectives>
    <inlineGenCodeDirectives>
        <directive>/*:GEN_CODE:*/</directive>
    </inlineGenCodeDirectives>
    <nestedLevelStartMarkers>
        <marker>{</marker>
    </nestedLevelStartMarkers>
    <nestedLevelEndMarkers>
        <marker>}</marker>
    </nestedLevelEndMarkers>
    <augCodeDirectives>
        <directive>//:AUG_CODE:</directive>
    </augCodeDirectives>
    <fileSets>
        <fileSet>
            <directory>src/main/java</directory>
            <includes>
              <include>**/*.java</include>
            </includes>
        </fileSet>
        <fileSet>
            <directory>src/test/java</directory>
            <includes>
              <include>**/*.java</include>
            </includes>
        </fileSet>
    </fileSets>
    <groovyEntryScriptName>main.groovy</groovyEntryScriptName>
    <groovyScriptDir>../codeAugmentorScripts</groovyScriptDir>
    <destDir>${project.build.directory}/codeAugmentor/generated</destDir>
    <codeChangeDetectionDisabled>false</codeChangeDetectionDisabled>
    <failOnChanges>true</failOnChanges>
</configuration>
```

*NB:*

   - **fileSets** collection cannot be empty, just as it is witn `code-augmentor:prepare` goal.
   - **groovyScriptDir** property is required.
   - When running `code-augmentor:run` goal, the following properties are hard-coded to their default values:
      - file of parse results of prepare stage (aka prepFile) is set to ${project.build.directory}/codeAugmentor/prepResults.json
      - file of augmenting codes produced by prepare stage and meant as input to process stage (aka augCodeFile) is set to ${project.build.directory}/codeAugmentor/augCodes.json
      - file of generated codes produced by process stage and meant as input to completion stage (aka genCodeFile) is set to ${project.build.directory}/codeAugmentor/genCodes.json

## Example POMs

### Intended for code-augmentor:run

`code-augmentor:run` provides the convenience of being easily hooked to a Maven build. Sample build file below demonstrates how to hook it to Maven's validate phase. As mentioned above though, Groovy is the only scripting platform that can be used with this goal.

#### POM file

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.aaronicsubstances</groupId>
  <artifactId>sample-app</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>jar</packaging>

  <build>
    <plugins>
      <plugin>
        <groupId>com.aaronicsubstances</groupId>
        <artifactId>code-augmentor-maven-plugin</artifactId>
        <version>1.0</version>
        <configuration>
            <verbose>false</verbose>
            <fileSets>
                <fileSet>
                    <directory>src</directory>
                    <includes>
                      <include>**/*.java</include>
                    </includes>
                </fileSet>
            </fileSets>
            <groovyScriptDir>../codeAugmentorScripts</groovyScriptDir>
        </configuration>
        <executions>
            <execution>
                <goals>
                    <goal>run</goal>
                </goals>
                <phase>validate</phase>
            </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

**NB:** [Examples](https://github.com/aaronicsubstances/code-augmentor/tree/master/examples) directory contains working maven plugin demonstration involving this build file.

### Not intended for code-augmentor:run

In this example we assume user wants to use NodeJS to run code generator scripts instead of Groovy. The maven command for which the POM file below is intended is:

```
mvn code-augmentor:prepare exec:exec code-augmentor:complete
```

#### POM file

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.example</groupId>
  <artifactId>code-augmentor-with-maven-demo</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>jar</packaging>
  
  <properties>
    <script.executable>node</script.executable>
  </properties>

  <build>
    <plugins>
      <plugin>
        <groupId>com.aaronicsubstances</groupId>
        <artifactId>code-augmentor-maven-plugin</artifactId>
        <version>2.0.0</version>
        <configuration>
            <verbose>true</verbose>
            <fileSets>
                <fileSet>
                    <directory>src</directory>
                    <includes>
                      <include>**/*.java</include>
                    </includes>
                </fileSet>
            </fileSets>
            <groovyScriptDir>../codeAugmentorScripts</groovyScriptDir>
        </configuration>
      </plugin>
      <plugin>
          <groupId>org.codehaus.mojo</groupId>
          <artifactId>exec-maven-plugin</artifactId>
          <version>3.0.0</version>
          <configuration>
              <executable>${script.executable}</executable>
              <arguments>
                  <arg>${project.build.directory}/codeAugmentor/augCodes.json</arg>
                  <arg>${project.build.directory}/codeAugmentor/genCodes.json</arg>
              </arguments>
          </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```
