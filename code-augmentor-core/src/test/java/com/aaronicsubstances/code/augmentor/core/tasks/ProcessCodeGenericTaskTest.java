package com.aaronicsubstances.code.augmentor.core.tasks;

import static org.testng.Assert.*;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.aaronicsubstances.code.augmentor.core.models.AugmentingCode;
import com.aaronicsubstances.code.augmentor.core.models.CodeGenerationResponse;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeGenericTask.EvalFunction;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ProcessCodeGenericTaskTest {
    
    @Test(dataProvider = "createTestExecuteData")
    public void testExecute(String inputPath, EvalFunction evalFunction,
            String expectedOutputPath, int expectedErrorCount) throws Exception {
        // transfer class path resources for original source files to
        // temp directory.
        File tempDir = new File(FileUtils.getTempDirectory(), getClass().getName());
        tempDir.mkdir();
        Charset taskCharset = StandardCharsets.UTF_8;
        String inputContents = TestResourceLoader.loadResource(inputPath, getClass());
        File inputFile = new File(tempDir, inputPath);
        FileUtils.write(inputFile, inputContents, taskCharset);

        String expectedOutput = TestResourceLoader.loadResource(expectedOutputPath, getClass());
        CodeGenerationResponse expectedResponse = CodeGenerationResponse.deserialize(
            new StringReader(expectedOutput));
        
        File actualOutputFile = new File(tempDir, expectedOutputPath);
    
        ProcessCodeGenericTask task = new ProcessCodeGenericTask();
        task.setJsonParseFunction(s -> null);
        task.setInputFile(inputFile);
        task.setOutputFile(actualOutputFile);
        task.execute(evalFunction);
        if (!task.getAllErrors().isEmpty()) {
            String stringifiedErrors = PluginUtils.stringifyPossibleScriptErrors(
                task.getAllErrors().stream().map(x -> (Throwable) x).collect(Collectors.toList()), 
                true, Arrays.asList("" + Integer.MAX_VALUE), Arrays.asList("-"));
            System.err.format("Errors from testing with %s:\n%s\n", inputPath, stringifiedErrors);
        }
        assertEquals(task.getAllErrors().size(), expectedErrorCount);
        if (task.getAllErrors().isEmpty()) {
            CodeGenerationResponse actualResponse = CodeGenerationResponse.deserialize(
                actualOutputFile);
            assertEquals(actualResponse, expectedResponse);
        }
    }

    @DataProvider
    public Object[][] createTestExecuteData() {
        EvalFunction evaler = (f, a, c) -> {
            return String.format("Received: %s(%s, %s)", f, a.getId(), c.getClass().getSimpleName());
        };
        EvalFunction evalerWithArrayResult = (f, a, c) -> {
            GeneratedCode genCode = c.newGenCode();
            genCode.setId(a.getId());
            genCode.getContentParts().add(c.newContent(
                String.format("Received: %s", f)));
            return Arrays.asList(genCode);
        };
        EvalFunction productionEvaler = (f, a, c) -> {
            int periodIndex = f.indexOf(".");
            String className = f.substring(0, periodIndex);
            String methodName = f.substring(periodIndex + 1);
            Class<?> cls = Class.forName(className);
            Method method = cls.getMethod(methodName, AugmentingCode.class, ProcessCodeContext.class);
            return method.invoke(null, a, c);
        };
        return new Object[][] {
            { "aug_codes-00.json", evaler, "expected_gen_codes-00.json", 0 },
            { "aug_codes-01.json", evalerWithArrayResult, "expected_gen_codes-01.json", 0 },
            { "aug_codes-02.json", productionEvaler, "dummy.json", 2 }
        };
    }
}