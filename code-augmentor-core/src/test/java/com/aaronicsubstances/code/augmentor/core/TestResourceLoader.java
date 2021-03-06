package com.aaronicsubstances.code.augmentor.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode;
import com.aaronicsubstances.code.augmentor.core.models.GeneratedCode.ContentPart;
import com.aaronicsubstances.code.augmentor.core.util.Token;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;

public class TestResourceLoader {
    public static final Random RAND_GEN = new Random();
    public static final Gson GSON_INST = new GsonBuilder().setPrettyPrinting().create();
    public static final Pattern NEW_LINE_REGEX = Pattern.compile("\r\n|\r|\n");

    public static String loadResource(String path, Class<?> cls) {
        return loadResourceWithEncoding(path, cls, StandardCharsets.UTF_8);
    }

    public static String loadResourceWithEncoding(String path, Class<?> cls,
            Charset encoding) {
        String pathPrefix = "";
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
            return IOUtils.toString(dummy, encoding);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String loadResourceNewlinesNormalized(String path,
            Class<?> cls, String newLine) {
        String text = loadResource(path, cls);
        return NEW_LINE_REGEX.matcher(text).replaceAll(newLine);
    }

    public static String loadResourceNewlinesNormalizedWithEncoding(String path,
            Class<?> cls, String newLine, Charset encoding) {
        String text = loadResourceWithEncoding(path, cls, encoding);
        return NEW_LINE_REGEX.matcher(text).replaceAll(newLine);
    }

	public static List<Token> fetchTokens(String path, Class<?> cls) {
		String serializedTokens = loadResource(path, cls);
		Token[] tokenList = GSON_INST.fromJson(serializedTokens, Token[].class);
		return Arrays.asList(tokenList);
    }

	public static List<ContentPart> loadContentParts(String path, Class<?> cls) {
        String serializedContentParts = loadResource(path, cls);
        ContentPart[] contentParts = GSON_INST.fromJson(serializedContentParts, ContentPart[].class);
		return Arrays.asList(contentParts);
	}
    
    public static List<ContentPart> generateRandomContentPartList(String input) {
        int maxContentPartSize = RAND_GEN.nextInt(30) + 1;
        List<ContentPart> contentParts = new ArrayList<>(maxContentPartSize);
        int start = 0;
        for (int i = 0; i < maxContentPartSize - 1; i++) {
            int randLen = 0;
            int remainderLen = input.length() - start;
            if (remainderLen > 0) {
                // generate from 0 up to and including input length.
                randLen = RAND_GEN.nextInt(remainderLen + 1);
            }
            int endIdx = start + randLen;
            if (randLen > 0) {
                // ensure we don't accidentally split a \r\n newline.
                if (input.charAt(endIdx - 1) == '\r') {
                    if (endIdx < input.length() && input.charAt(endIdx) == '\n') {
                        endIdx++;
                    }
                }
            }
            contentParts.add(new ContentPart(input.substring(start, endIdx), 
                RAND_GEN.nextBoolean()));
            start = endIdx;
        }
        if (contentParts.isEmpty() || start < input.length() || RAND_GEN.nextBoolean()) {
            contentParts.add(new ContentPart(input.substring(start), 
                RAND_GEN.nextBoolean()));
        }
        assert new GeneratedCode(contentParts).getWholeContent().equals(input);
        return contentParts;
    } 

    public static String craftErrorMessageInvolvingRandomContentParts(List<ContentPart> inputContentParts,
            Object expected, Object actual) {        
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("After split yielded:\n");
        errorMessage.append(TestResourceLoader.GSON_INST.toJson(inputContentParts));
        errorMessage.append("\n\nExpectation:\n");
        errorMessage.append(TestResourceLoader.GSON_INST.toJson(expected));
        errorMessage.append("\n\nActual:\n");
        errorMessage.append(TestResourceLoader.GSON_INST.toJson(actual));
        errorMessage.append("\n\n");
        return errorMessage.toString();
    }

	public static void printTestHeader(String testName, Object... testArgs) {
        System.out.print(testName + "(");
        for (int i = 0; i < testArgs.length; i++) {
            if (i > 0) {
                System.out.print(", ");
            }
            if (testArgs[i] != null) {
                System.out.print(testArgs[i]);
            }
        }
        System.out.println(")");
        System.out.println("---------------------");
    }

    public static <K, V> KeyValuePair<K, V> newMapEntry(K key, V value) {
        return new KeyValuePair<>(key, value);
    }

    public static <K, V> Map<K, V> newMap(List<KeyValuePair<K, V>> entries) {
        Map<K, V> map = new HashMap<>();
        for (KeyValuePair<K, V> entry : entries) {
            map.put(entry.key, entry.value);
        }
        return map;
    }

    public static class KeyValuePair<K, V> {
        public K key;
        public V value;

        public KeyValuePair(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}