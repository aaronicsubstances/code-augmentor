package com.aaronicsubstances.code.augmentor.core.util;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;
import com.google.gson.Gson;

public class SourceCodeTokenizerTest {

	public static List<Token> fetchTokens(String sourceName) {
		String serializedTokens = TestResourceLoader.loadResource(sourceName,
			SourceCodeTokenizerTest.class);
		Token[] tokenList = new Gson().fromJson(serializedTokens, Token[].class);
		return Arrays.asList(tokenList);
	}

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

		SourceCodeTokenizer instance = new SourceCodeTokenizer(
			genCodeStartDirectives, genCodeEndDirectives, 
			embeddedStringDirectives, embeddedJsonDirectives, 
			enableScanDirectives, disableScanDirectives, 
			augCodeDirectiveSets);
		return instance;
	}

	@Test(dataProvider = "createTestTokenizeSourceData")
	public void testTokenizeSource(String inputSourceName, String expectedTokenSourceName) {
		List<Token> expected = fetchTokens(expectedTokenSourceName);
		String input = TestResourceLoader.loadResourceNewlinesNormalized(inputSourceName,
			getClass(), "\r\n");
		SourceCodeTokenizer instance = createInstance();
		List<Token> actual = instance.tokenizeSource(input);
		assertEquals(actual, expected);
	}

	@DataProvider
	public Object[][] createTestTokenizeSourceData() {
		return new Object[][]{
			{ "sample-code.php", "sample-code.php.json" }
		};
	}
}