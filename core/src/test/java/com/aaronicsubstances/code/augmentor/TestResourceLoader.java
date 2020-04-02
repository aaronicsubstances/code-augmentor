package com.aaronicsubstances.code.augmentor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import com.aaronicsubstances.code.augmentor.parsing.LexerSupport;
import com.aaronicsubstances.code.augmentor.parsing.Token;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

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
        return newText;
	}
    
    public static List<Token> deserializeTokens(int i, Class<?> cls) {
        String path = String.format("tokens-%02d.json", i);
        String text = TestResourceLoader.loadResource(path, cls);
        Gson gson = new Gson();
        TokenLike[] tokens = gson.fromJson(text, TokenLike[].class);
        
        return Arrays.stream(tokens).map(t -> t.toToken()).collect(Collectors.toList());
    }

    public static class TokenLike {
        public int type;
        public String text;
        public int startPos;
        public int endPos;
        public int lineNumber;
        public Value value;

        public static class Value {
            @SerializedName("import")
            public String _import;
            public boolean ws_reqd;
        }

        public Token toToken() {
            Token t = new Token(type, text, startPos, endPos, lineNumber);
            if (value != null) {
                t.value = new HashMap<>();
                if (value._import != null) {
                    t.value.put(Token.VALUE_KEY_IMPORT_STATEMENT, value._import);
                }
                if (value.ws_reqd) {
                    t.value.put(Token.VALUE_KEY_WS_REQD, true);
                }
            }
            return t;
        }
    }
}