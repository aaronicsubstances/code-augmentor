package com.aaronicsubstances.programmer.companion.ant.plugin.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import com.aaronicsubstances.programmer.companion.ParserInputSource;
import com.aaronicsubstances.programmer.companion.Token;
import com.aaronicsubstances.programmer.companion.java.JavaParser;
import com.aaronicsubstances.programmer.companion.java.JavaSourceCodeWrapper;
import com.aaronicsubstances.programmer.companion.kotlin.KotlinParser;

import org.apache.tools.ant.util.FileUtils;

/**
 * TaskUtils
 */
public class TaskUtils {

    public static String getFileExt(String path) {
        int index = path.lastIndexOf(".");
        if (index != -1) {
            return path.substring(index + 1);
        }
        return "";
    }

    public static String readFile(File srcFile, Charset charset) throws IOException {
        try (Reader rdr = new InputStreamReader(new FileInputStream(srcFile), charset)) {
            return FileUtils.readFully(rdr);
        }
    }

    public static void writeFile(File destFile, Charset charset, String contents) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(destFile), charset)) {
            writer.write(contents);
        }
    }

    public static ParserInputSource parseSourceCode(String relativePath, String input, List<Token> receiver) {
        // use file extension to parse as Java/Kotlin code.
        String fileExt = getFileExt(relativePath);
        if ("java".equals(fileExt)) {
            JavaSourceCodeWrapper inputSource = new JavaSourceCodeWrapper(input);
            JavaParser instance = new JavaParser(inputSource);
            receiver.addAll(instance.parse());
            return inputSource;
        } else if ("kt".equals(fileExt)) {
            ParserInputSource inputSource = new ParserInputSource(input);
            KotlinParser instance = new KotlinParser(inputSource);
            receiver.addAll(instance.parse());
            return inputSource;
        } else {
            throw new RuntimeException("Unexpected file extension in " + relativePath);
        }
    }

    public static boolean isNullOrEmpty(String indent) {
        return indent == null || indent.isEmpty();
    }

    public static String calcHash(String contents, Charset charset) throws NoSuchAlgorithmException {
        byte[] binaryContent = contents.getBytes(charset);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(binaryContent);
        binaryContent = md.digest();
        String hash = Base64.getEncoder().encodeToString(binaryContent);
        return hash;
    }
}