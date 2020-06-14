@GrabConfig(systemClassLoader=true) // needed so -cp deps can find grabbed deps
@Grab('com.google.code.gson:gson:2.8.6')

import com.aaronicsubstances.code.augmentor.ant.CodeAugmentorTask
import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask

import groovy.json.JsonSlurper

import java.text.NumberFormat

import org.junit.Before
import org.junit.Test
import org.junit.runner.JUnitCore
import org.junit.runner.notification.RunListener

import static groovy.test.GroovyAssert.*

class MyTestCase {

    /**
     * This adds the single requirement of exiting test run with failure exit code of 1 
     * if some tests failed or were ignored.
     *
     */
    public static void main(String[] args) {
        def testRunner = new JUnitCore()
        def testResultReporter = [
            testStarted: {
                println "\n\n$it"
            },
            testRunFinished: {
                // print header
                println()
                println "Time: " + NumberFormat.getInstance().format((double) it.runTime / 1000) + " secs"
                
                // print failures
                if (it.failureCount) {
                    println "There were $it.failureCount failure(s)"
                    it.failures.eachWithIndex { f, i ->
                        println "$i) $f.testHeader"
                        println "$f.trace"
                    }
                }
                
                // print footer
                println()
                println(it.wasSuccessful() ? "OK" : "FAILURES!!!")
                int successCount = it.runCount - it.failureCount - it.ignoreCount
                def summary = "Tests run: $it.runCount, Passed: $successCount, " +
                    "Failed: $it.failureCount, Ignored: $it.ignoreCount"
                println(summary)
                println()
            }
        ] as RunListener
        testRunner.addListener(testResultReporter)
        def result = testRunner.run(MyTestCase.class)
        if (result.failureCount || result.ignoreCount || result.ignoreCount) {
            System.exit(1)
        }
    }
    
    def ant
    
    @Before
    void setUp() {
        ant = new AntBuilder()
        
        ant.taskdef(resource: "CodeAugmentorTasks.xml", onerror: "failall")
        
        ant.project.addReference(CodeAugmentorTask.PROJECT_REFERENCE_DEFAULT_STACK_TRACE_LIMIT_PREFIXES, 
            [ getClass().name ] )

        def jsonParser = new JsonSlurper()
        ProcessCodeGenericTask.JsonParseFunction jsonParseFunction = {
            return jsonParser.parseText(it)
        }
        ant.project.addReference(CodeAugmentorTask.PROJECT_REFERENCE_JSON_PARSE_FUNCTION,
            jsonParseFunction)

        
        ProcessCodeGenericTask.EvalFunction scriptEvalFunction = { functionName, augCode, context ->
            def binding = new Binding()
            def groovyShell = new GroovyShell(binding)
            binding.augCode = augCode
            binding.context = context
            return groovyShell.evaluate(functionName + '(augCode, context)')
        }
        ant.project.addReference(CodeAugmentorTask.PROJECT_REFERENCE_SCRIPT_EVAL_FUNCTION,
            scriptEvalFunction)
    }
    
    @Test
    void testDoNotDetectChange() {
        ant.code_aug_run(verbose: true, codeChangeDetectionDisabled: true) {
            srcDir(dir: 'src') {
                include(name: '**/*.java')
            }
        }
    }
    
    @Test
    void testCodeChangeAbsent() {
        ant.code_aug_run(verbose: true) {
            srcDir(dir: 'src') {
                include(name: '**/*.java')
                exclude(name: 'com/Main2.java')
            }
        }
    }
    
    @Test
    void testCodeChangePresent() {
        shouldFail {
            ant.code_aug_run(verbose: true) {
                srcDir(dir: 'src') {
                    include(name: '**/*.java')
                }
            }
        }
    }
    
    @Test
    void testCodeChangePresentDF() {
        ant.code_aug_run(verbose: true, failOnChanges: false) {
            srcDir(dir: 'src') {
                include(name: '**/*.java')
            }
        }
    }
}