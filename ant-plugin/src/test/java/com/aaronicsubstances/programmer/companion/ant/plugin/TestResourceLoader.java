package com.aaronicsubstances.programmer.companion.ant.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

import com.aaronicsubstances.programmer.companion.LexerSupport;

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
        System.out.println("Resource path: " + eventualPath);
        InputStream resourceStream = TestResourceLoader.class.getClassLoader().getResourceAsStream(
            eventualPath);
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
        Matcher m = LexerSupport.NEW_LINE_REGEX.matcher(text);
        StringBuffer sb = new StringBuffer();
        String replacement = Matcher.quoteReplacement(newLine);
        while (m.find()) {
            if (m.group().equals(newLine)) {
                continue;
            }
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        String newText = sb.toString();
        /*System.out.println("Before new lines normalized: " + text);
        System.out.println("After new lines normalized: " + newText);*/
        return newText;
	}
}