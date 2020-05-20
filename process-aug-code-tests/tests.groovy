@Grab('com.aaronicsubstances:code-augmentor-core:1.0-SNAPSHOT')

import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationResponse

import org.junit.Test
import org.junit.BeforeClass

import static groovy.test.GroovyAssert.*

class MyTestCase {
    static scriptDir, buildDir
    static verboseLoggingEnabled = true

    static final PRINT_OUT = "printOut"
    static final EXIT_RESULT = "exitResult"

    @BeforeClass
    static void setUpClass() {
        scriptDir = new File(MyTestCase.class.protectionDomain.codeSource.location.path).parent
        buildDir = new File(scriptDir, 'build')
        def ant = new AntBuilder()
        ant.delete(dir: buildDir)

        // set up nodejs scripts.
        ant.copy(todir: "$buildDir/nodejs", file: "$LocalConfig.NODEJS_REPO_PATH/index.js")

        // set up python scripts.
        ant.copy(todir: "$buildDir/python3") {
            fileset(dir: "$LocalConfig.PYTHON_REPO_PATH/code_augmentor_support") {
                include(name: 'models.py')
                include(name: 'tasks.py')
                include(name: '__init__.py')
            }
        }

        // set up PHP scripts.
        ant.copy(todir: "$buildDir/php7") {
            fileset(dir: "$LocalConfig.PHP_REPO_PATH/src")
        }

        // set up java/groovy scripts.
        ant.copy(todir: "$buildDir/java", 
                file: "../code-augmentor-app/src/test/projects/basic/main.groovy")
        ant.copy(todir: "$buildDir/java", 
                file: "Snippets.groovy")
    }

    @Test
    void testAssertions() {
        assertTrue(1 == 1)
        assertEquals("test", "test")

        def x = "42"
        assertNotNull "x must not be null", x
        assertNull null

        assertSame x, x
    }
    
    @Test
    void testProcessCodeTasks() {
        testSingleRun("augCodes-01.json", 0, 0, "genCodes-01.json")
        testSingleRun("augCodes-02.json", 0, 0, "genCodes-02.json")
        testSingleRun("augCodes-03.json", 0, 0, "genCodes-03.json")
    }

    void testSingleRun(String inputFileName, int successExitCode, int expectedErrorCount,
            String expectedOutputFileName) {
        0.upto(3) {
            def actualOutputFile
            def ant
            switch (it) {
                case 0:
                    actualOutputFile = new File(buildDir, inputFileName + "-out-js")
                    ant = execOnNodeJs(inputFileName, actualOutputFile)
                    break;
                case 1:
                    actualOutputFile = new File(buildDir, inputFileName + "-out-php")
                    ant = execOnPHP(inputFileName, actualOutputFile)
                    break;
                case 2:
                    actualOutputFile = new File(buildDir, inputFileName + "-out-py3")
                    ant = execOnPython3(inputFileName, actualOutputFile)
                    break;
                case 3:
                    actualOutputFile = new File(buildDir, inputFileName + "-out-java")
                    ant = execOnGroovy(inputFileName, actualOutputFile)
                    break;
                default:
                    throw new Exception("Unexpected index: $it")
            }

            assert ant.project.getProperty(EXIT_RESULT) == "$successExitCode" : 
                ant.project.getProperty(PRINT_OUT)
        
            def m = ant.project.getProperty(PRINT_OUT) =~ /(?i)\d+ error\(s\) found/
            if (expectedErrorCount) {
                assert m.group() == "$expectedErrorCount error(s) found"
                assert m
            }
            else {
                assert !m
            }
            if (expectedOutputFileName != null) {
                def expectedGenCode = CodeGenerationResponse.deserialize(
                    new File(scriptDir, "resources/$expectedOutputFileName"))
                def actualGenCode = CodeGenerationResponse.deserialize(
                    actualOutputFile)
                assertEquals(expectedGenCode, actualGenCode)
            }
        }
    }

    def execOnNodeJs(inputFileName, outputFile) {
        def ant = new AntBuilder()
        ant.echo("execOnNodeJs with $inputFileName")
        ant.exec(executable: 'node', vmlauncher: false, dir: scriptDir,
                errorproperty: PRINT_OUT, resultproperty: EXIT_RESULT) {
            arg(value: "main.js")
            arg(value: "resources/$inputFileName")
            arg(value: "$outputFile")
            if (verboseLoggingEnabled){
                arg(value: "true")
            }
        }
        return ant
    }

    def execOnPHP(inputFileName, outputFile) {
        def ant = new AntBuilder()
        ant.echo("execOnPHP with $inputFileName")
        ant.exec(executable: 'php', vmlauncher: false, dir: scriptDir,
                errorproperty: PRINT_OUT, resultproperty: EXIT_RESULT) {
            arg(value: "main.php")
            arg(value: "resources/$inputFileName")
            arg(value: "$outputFile")
            if (verboseLoggingEnabled){
                arg(value: "true")
            }
        }
        return ant
    }

    def execOnPython3(inputFileName, outputFile) {
        def ant = new AntBuilder()
        ant.echo("execOnPython3 with $inputFileName")
        ant.exec(executable: LocalConfig.PYTHON_EXEC, vmlauncher: false, dir: scriptDir,
                errorproperty: PRINT_OUT, resultproperty: EXIT_RESULT) {
            arg(value: "-B")
            arg(value: "main.py")
            arg(value: "resources/$inputFileName")
            arg(value: "$outputFile")
            if (verboseLoggingEnabled){
                arg(value: "true")
            }
        }
        return ant
    }

    def execOnGroovy(inputFileName, outputFile) {        
        def ant = new AntBuilder()
        ant.echo("execOnGroovy with $inputFileName")
        ant.exec(executable: 'groovy', vmlauncher: false, dir: scriptDir,
                errorproperty: PRINT_OUT, resultproperty: EXIT_RESULT) {
            arg(value: "build/java/main.groovy")
            arg(value: "resources/$inputFileName")
            arg(value: "$outputFile")
            if (verboseLoggingEnabled){
                arg(value: "true")
            }
        }
        return ant
    }
}