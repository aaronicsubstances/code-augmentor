package com.aaronicsubstances.code.augmentor.core.tasks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import com.aaronicsubstances.code.augmentor.core.parsing.TokenSupplier;
import com.aaronicsubstances.code.augmentor.core.parsing.java.JavaParser;
import com.aaronicsubstances.code.augmentor.core.parsing.kotlin.KotlinParser;

/**
 * Exposes helper methods for generic tasks
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
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try (InputStream inStream = new FileInputStream(srcFile)) {
            copyStream(inStream, outStream);
            outStream.flush();
        }
        byte[] buf = outStream.toByteArray();
        String s = new String(buf, charset);
        return s;
    }

    public static void writeFile(File destFile, Charset charset, String contents) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(destFile), charset)) {
            writer.write(contents);
        }
    }

    public static TokenSupplier parseSourceCode(String relativePath, String input) {
        // use file extension to parse as Java/Kotlin code.
        String fileExt = getFileExt(relativePath);
        switch (fileExt) {
            case "java":
                return new JavaParser(input);
            case "kt":
                return new KotlinParser(input);
            default:            
                throw new GenericTaskException("Unexpected file extension in " + relativePath);
        }
    }

    public static String calcHash(String contents, Charset charset) throws NoSuchAlgorithmException {
        byte[] binaryContent = contents.getBytes(charset);
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(binaryContent);
        binaryContent = md.digest();
        String hash = Base64.getEncoder().encodeToString(binaryContent);
        return hash;
    }

    public static void copyStream(InputStream inStream, OutputStream outStream) throws IOException {
        byte[] b  = new byte[8192];
        int len;
        while ((len = inStream.read(b)) > 0) {
            outStream.write(b, 0, len);
        }
    }

	public static void copyFile(File srcFile, File destFile) throws IOException {
        try (InputStream inputStream = new FileInputStream(srcFile)) {
            try (OutputStream outputStream = new FileOutputStream(destFile)) {
                copyStream(inputStream, outputStream);
            }
        }
	}
}