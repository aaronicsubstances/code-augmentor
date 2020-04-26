package com.aaronicsubstances.code.augmentor.core.util;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;

public class SourceCodeTokenizerTest {

	private static SourceCodeTokenizer createInstance() {
		List<String> genCodeStartDirectives = Arrays.asList("#GS");
		List<String> genCodeEndDirectives = Arrays.asList("#GE");
		List<String> embeddedStringDirectives = Arrays.asList("#ES");

		List<String> embeddedJsonDirectives = Arrays.asList("#ARG");

		// add test of longest directive.
		List<String> enableScanDirectives = Arrays.asList("//--<<");
		List<String> disableScanDirectives = Arrays.asList("//--");

		// add test for longest directive between different aug
		// codes.
		List<List<String>> augCodeDirectiveSets = Arrays.asList(
			Arrays.asList("#PHP"),
			Arrays.asList("#PHP5", "#PHP7")
		);

		List<String> inlineGenCodeDirectives = Arrays.asList("#GG#");

		List<String> nestedLevelStartMarkers = Arrays.asList("[[", "{{", "((");
		List<String> nestedLevelEndMarkers = Arrays.asList("]]", "}}", "))");

		SourceCodeTokenizer instance = new SourceCodeTokenizer(
			genCodeStartDirectives, genCodeEndDirectives, 
			embeddedStringDirectives, embeddedJsonDirectives, 
			enableScanDirectives, disableScanDirectives, 
			augCodeDirectiveSets, inlineGenCodeDirectives,
			nestedLevelStartMarkers, nestedLevelEndMarkers);
		return instance;
	}

	@Test(dataProvider = "createTestTokenizeSourceData")
	public void testTokenizeSource(String inputSourceName, String newline, String expectedTokenSourceName) {
		List<Token> expected = TestResourceLoader.fetchTokens(expectedTokenSourceName,
			getClass());
		String input = TestResourceLoader.loadResourceNewlinesNormalized(inputSourceName,
			getClass(), newline);
		SourceCodeTokenizer instance = createInstance();
		List<Token> actual = instance.tokenizeSource(input);
		assertEquals(actual, expected);
	}

	@DataProvider
	public Object[][] createTestTokenizeSourceData() {
		return new Object[][]{
			{ "sample-code.php", "\r\n", "sample-code.php.json" },
			{ "sample-code.java", "\r\n", "sample-code.java.json" },
			{ "sample-code-on-unix.php", "\n", "sample-code-on-unix.php.json" }
		};
	}
}