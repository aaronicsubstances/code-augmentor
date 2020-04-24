package com.aaronicsubstances.code.augmentor.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.util.TaskUtils;
import com.aaronicsubstances.code.augmentor.core.util.Token;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

public class TestResourceLoader {

    public static String loadResource(String path, Class<?> cls) {
        String pathPrefix = "";;
        if (cls != null) {
            int pkgNameLength = TestResourceLoader.class.getPackage().getName().length();
            pathPrefix = cls.getName().substring(pkgNameLength + 1).replace(".", "/") + "/";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String eventualPath = pathPrefix + path;
        InputStream resourceStream = TestResourceLoader.class.getClassLoader().getResourceAsStream(
            eventualPath);
        if (resourceStream == null) {
            throw new RuntimeException("Could not classpath resource " + eventualPath);
        }
        try (InputStream dummy = resourceStream) {
            return IOUtils.toString(dummy, StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String loadResourceNewlinesNormalized(String path,
            Class<?> cls, String newLine) {
        String text = loadResource(path, cls);
        List<String> splitText = TaskUtils.splitIntoLines(text);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < splitText.size(); i+=2) {
            String line = splitText.get(i);
            sb.append(line);
            String terminator = splitText.get(i + 1);
            if (terminator == null) {
                break;
            }
            sb.append(newLine);
        }
        String newText = sb.toString();
        return newText;
    }

	public static List<Token> fetchTokens(String path, Class<?> cls) {
		String serializedTokens = TestResourceLoader.loadResource(path,
			cls);
		Token[] tokenList = new Gson().fromJson(serializedTokens, Token[].class);
		return Arrays.asList(tokenList);
	}
}