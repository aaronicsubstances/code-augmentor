package com.aaronicsubstances.code.augmentor.core.models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.util.PersistenceUtil;

/**
 * Represents input file to processing stage of Code Augmentor.
 */
public class CodeGenerationRequest {

    static class Header {
        Boolean contentStreamingEnabled;

        String genCodeStartDirective;
        String genCodeEndDirective;
        String embeddedStringDirective;
        String embeddedJsonDirective;
        String skipCodeStartDirective;
        String skipCodeEndDirective;
        String augCodeDirective;
        String inlineGenCodeDirective;
        String nestedLevelStartMarker;
        String nestedLevelEndMarker;
    }

    private String genCodeStartDirective;
    private String genCodeEndDirective;
    private String embeddedStringDirective;
    private String embeddedJsonDirective;
    private String skipCodeStartDirective;
    private String skipCodeEndDirective;
    private String augCodeDirective;
    private String inlineGenCodeDirective;
    private String nestedLevelStartMarker;
    private String nestedLevelEndMarker;

    public String getGenCodeStartDirective() {
        return genCodeStartDirective;
    }

    /**
     * Sets in {@link com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeContext#getHeader()}
     * the first of generated code start directives used to configure preparation stage. Set to 
     * null if no such directives were provided.
     * @param genCodeStartDirective
     */
    public void setGenCodeStartDirective(String genCodeStartDirective) {
        this.genCodeStartDirective = genCodeStartDirective;
    }

    public String getGenCodeEndDirective() {
        return genCodeEndDirective;
    }

    /**
     * Sets in {@link com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeContext#getHeader()}
     * the first of generated code end directives used to configure preparation stage. Set to 
     * null if no such directives were provided.
     * @param genCodeEndDirective
     */
    public void setGenCodeEndDirective(String genCodeEndDirective) {
        this.genCodeEndDirective = genCodeEndDirective;
    }

    public String getEmbeddedStringDirective() {
        return embeddedStringDirective;
    }

    /**
     * Sets in {@link com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeContext#getHeader()}
     * the first of embedded string directives used to configure preparation stage. Set to 
     * null if no such directives were provided.
     * @param embeddedStringDirective
     */
    public void setEmbeddedStringDirective(String embeddedStringDirective) {
        this.embeddedStringDirective = embeddedStringDirective;
    }

    public String getEmbeddedJsonDirective() {
        return embeddedJsonDirective;
    }

    /**
     * Sets in {@link com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeContext#getHeader()}
     * the first of embedded JSON directives used to configure preparation stage. Set to 
     * null if no such directives were provided.
     * @param embeddedJsonDirective
     */
    public void setEmbeddedJsonDirective(String embeddedJsonDirective) {
        this.embeddedJsonDirective = embeddedJsonDirective;
    }

    public String getSkipCodeStartDirective() {
        return skipCodeStartDirective;
    }

    /**
     * Sets in {@link com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeContext#getHeader()}
     * the first of skip start directives used to configure preparation stage. Set to 
     * null if no such directives were provided.
     * @param skipCodeStartDirective
     */
    public void setSkipCodeStartDirective(String skipCodeStartDirective) {
        this.skipCodeStartDirective = skipCodeStartDirective;
    }

    public String getSkipCodeEndDirective() {
        return skipCodeEndDirective;
    }

    /**
     * Sets in {@link com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeContext#getHeader()}
     * the first of skip end directives used to configure preparation stage. Set to 
     * null if no such directives were provided.
     * @param skipCodeEndDirective
     */
    public void setSkipCodeEndDirective(String skipCodeEndDirective) {
        this.skipCodeEndDirective = skipCodeEndDirective;
    }

    public String getAugCodeDirective() {
        return augCodeDirective;
    }

    /**
     * Sets in {@link com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeContext#getHeader()}
     * the first of aug code directives used to configure preparation stage and is
     * associated with this code generation request.
     * @param augCodeDirective
     */
    public void setAugCodeDirective(String augCodeDirective) {
        this.augCodeDirective = augCodeDirective;
    }

    public String getInlineGenCodeDirective() {
        return inlineGenCodeDirective;
    }

    /**
     * Sets in {@link com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeContext#getHeader()}
     * the first of inline generated code directives used to configure preparation stage. Set to 
     * null if no such directives were provided.
     * @param inlineGenCodeDirective
     */
    public void setInlineGenCodeDirective(String inlineGenCodeDirective) {
        this.inlineGenCodeDirective = inlineGenCodeDirective;
    }

    public String getNestedLevelStartMarker() {
        return nestedLevelStartMarker;
    }

    /**
     * Sets in {@link com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeContext#getHeader()}
     * the first of nested level start markers used to configure preparation stage. Set to 
     * null if no such directives were provided.
     * @param nestedLevelStartMarker
     */
    public void setNestedLevelStartMarker(String nestedLevelStartMarker) {
        this.nestedLevelStartMarker = nestedLevelStartMarker;
    }

    public String getNestedLevelEndMarker() {
        return nestedLevelEndMarker;
    }

    /**
     * Sets in {@link com.aaronicsubstances.code.augmentor.core.tasks.ProcessCodeContext#getHeader()}
     * the first of nested level end markers used to configure preparation stage. Set to 
     * null if no such directives were provided.
     * @param nestedLevelEndMarker
     */
    public void setNestedLevelEndMarker(String nestedLevelEndMarker) {
        this.nestedLevelEndMarker = nestedLevelEndMarker;
    }

    private List<SourceFileAugmentingCode> sourceFileAugmentingCodes;

    public CodeGenerationRequest() {
    }

    public CodeGenerationRequest(List<SourceFileAugmentingCode> sourceFileAugmentingCodes) {
        this.sourceFileAugmentingCodes = sourceFileAugmentingCodes;
    }

    public List<SourceFileAugmentingCode> getSourceFileAugmentingCodes() {
        return sourceFileAugmentingCodes;
    }

    public void setSourceFileAugmentingCodes(List<SourceFileAugmentingCode> sourceFileAugmentingCodes) {
        this.sourceFileAugmentingCodes = sourceFileAugmentingCodes;
    }

    public Object beginSerialize(File file) throws Exception {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file),
            StandardCharsets.UTF_8);
        return beginSerialize(writer, true);
    }

    public Object beginSerialize(Writer stream) throws Exception {
        return beginSerialize(stream, false);
    }

    private PersistenceUtil beginSerialize(Writer stream, boolean closeStream) throws Exception {
        PersistenceUtil persistenceUtil= new PersistenceUtil(new BufferedWriter(stream), closeStream);
        printHeader(persistenceUtil, true);
        return persistenceUtil;
    }

    private void printHeader(PersistenceUtil persistenceUtil, boolean contentStreamEnabled) 
            throws Exception {
        Header header = new Header();
        header.genCodeStartDirective = genCodeStartDirective;
        header.genCodeEndDirective = genCodeEndDirective;
        header.skipCodeStartDirective = skipCodeStartDirective;
        header.skipCodeEndDirective = skipCodeEndDirective;
        header.embeddedStringDirective = embeddedStringDirective;
        header.embeddedJsonDirective = embeddedJsonDirective;
        header.augCodeDirective = augCodeDirective;
        header.inlineGenCodeDirective = inlineGenCodeDirective;
        header.nestedLevelStartMarker = nestedLevelStartMarker;
        header.nestedLevelEndMarker = nestedLevelEndMarker;
        // try not to set contentStreamEnabled to true since it's true by default.
        if (!contentStreamEnabled) {
            header.contentStreamingEnabled = false;
        }
        String headerString = PersistenceUtil.serializeCompactlyToJson(header);
        persistenceUtil.println(headerString);
    }

    public void endSerialize(Object serializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) serializer;
        try {
            persistenceUtil.flush();
        }
        finally {
            persistenceUtil.close();
        }
    }

    public void serialize(File file, boolean serializeAllAsJson) throws Exception {        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file),
                StandardCharsets.UTF_8)) {
            serialize(writer, serializeAllAsJson);
        }
    }

    public void serialize(Writer stream, boolean serializeAllAsJson) throws Exception {
        PersistenceUtil persistenceUtil = new PersistenceUtil(new BufferedWriter(stream), false);
        printHeader(persistenceUtil, !serializeAllAsJson);
        if (serializeAllAsJson) {
            String json = PersistenceUtil.serializeFormattedToJson(sourceFileAugmentingCodes);
            persistenceUtil.println(json);
        }
        else {
            for (SourceFileAugmentingCode s : sourceFileAugmentingCodes) {
                s.serialize(persistenceUtil);
            }
        }
        persistenceUtil.flush();
    }

    public Object beginDeserialize(File file) throws Exception {
        Reader reader = new InputStreamReader(new FileInputStream(file),
            StandardCharsets.UTF_8);
        return beginDeserialize(reader, true);
    }

    public Object beginDeserialize(File file, StringBuilder headerLineReceiver) throws Exception {
        Reader reader = new InputStreamReader(new FileInputStream(file),
            StandardCharsets.UTF_8);
        return beginDeserialize(reader, true, headerLineReceiver);
    }

    public Object beginDeserialize(Reader reader) throws Exception {
        return beginDeserialize(reader, false, null);
    }

    public PersistenceUtil beginDeserialize(Reader stream, boolean closeStream) throws Exception {
        return beginDeserialize(stream, closeStream, null);
    }

    public PersistenceUtil beginDeserialize(Reader stream, boolean closeStream,
            StringBuilder headerLineReceiver) throws Exception {
        PersistenceUtil persistenceUtil = new PersistenceUtil(new BufferedReader(stream), 
            closeStream);
        boolean contentStreamEnabled = readHeader(persistenceUtil, headerLineReceiver);
        sourceFileAugmentingCodes = new ArrayList<>();
        if (!contentStreamEnabled) {            
            String inputRemainder = persistenceUtil.readToEnd();
            SourceFileAugmentingCode[] entireList = PersistenceUtil.deserializeFromJson(inputRemainder, 
                SourceFileAugmentingCode[].class);
            persistenceUtil.setContent(entireList);
        }
        return persistenceUtil;
    }

    private boolean readHeader(PersistenceUtil persistenceUtil, 
            StringBuilder headerLineReceiver) throws Exception {
        String headerString = persistenceUtil.readLine();
        if (headerLineReceiver != null) {
            headerLineReceiver.append(headerString);
        }
        Header header = PersistenceUtil.deserializeFromJson(headerString, Header.class);
        genCodeStartDirective = header.genCodeStartDirective;
        genCodeEndDirective = header.genCodeEndDirective;
        skipCodeStartDirective = header.skipCodeStartDirective;
        skipCodeEndDirective = header.skipCodeEndDirective;
        embeddedStringDirective = header.embeddedStringDirective;
        embeddedJsonDirective = header.embeddedJsonDirective;
        augCodeDirective = header.augCodeDirective;
        inlineGenCodeDirective = header.inlineGenCodeDirective;
        nestedLevelStartMarker = header.nestedLevelStartMarker;
        nestedLevelEndMarker = header.nestedLevelEndMarker;
        // enable content streaming by default.
        boolean contentStreamingEnabled = true;
        if (header.contentStreamingEnabled != null) {
            contentStreamingEnabled = header.contentStreamingEnabled;
        }
        return contentStreamingEnabled;
    }

    public void endDeserialize(Object deserializer) throws Exception {
        PersistenceUtil persistenceUtil = (PersistenceUtil) deserializer;
        persistenceUtil.close();
    }

    public static CodeGenerationRequest deserialize(File file) throws Exception {
        try (Reader reader = new InputStreamReader(new FileInputStream(file),
                StandardCharsets.UTF_8)) {
            return deserialize(reader);
        }
    }

    public static CodeGenerationRequest deserialize(Reader reader) throws Exception {
        CodeGenerationRequest instance = new CodeGenerationRequest();
        Object deserializer = instance.beginDeserialize(reader);
        try {
            SourceFileAugmentingCode s;
            while ((s = SourceFileAugmentingCode.deserialize(deserializer)) != null) {
                instance.sourceFileAugmentingCodes.add(s);
            }
        }
        finally {
            instance.endDeserialize(deserializer);
        }
        return instance;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((augCodeDirective == null) ? 0 : augCodeDirective.hashCode());
        result = prime * result + ((embeddedJsonDirective == null) ? 0 : embeddedJsonDirective.hashCode());
        result = prime * result + ((embeddedStringDirective == null) ? 0 : embeddedStringDirective.hashCode());
        result = prime * result + ((genCodeEndDirective == null) ? 0 : genCodeEndDirective.hashCode());
        result = prime * result + ((genCodeStartDirective == null) ? 0 : genCodeStartDirective.hashCode());
        result = prime * result + ((inlineGenCodeDirective == null) ? 0 : inlineGenCodeDirective.hashCode());
        result = prime * result + ((nestedLevelEndMarker == null) ? 0 : nestedLevelEndMarker.hashCode());
        result = prime * result + ((nestedLevelStartMarker == null) ? 0 : nestedLevelStartMarker.hashCode());
        result = prime * result + ((skipCodeEndDirective == null) ? 0 : skipCodeEndDirective.hashCode());
        result = prime * result + ((skipCodeStartDirective == null) ? 0 : skipCodeStartDirective.hashCode());
        result = prime * result + ((sourceFileAugmentingCodes == null) ? 0 : sourceFileAugmentingCodes.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CodeGenerationRequest other = (CodeGenerationRequest) obj;
        if (augCodeDirective == null) {
            if (other.augCodeDirective != null)
                return false;
        } else if (!augCodeDirective.equals(other.augCodeDirective))
            return false;
        if (embeddedJsonDirective == null) {
            if (other.embeddedJsonDirective != null)
                return false;
        } else if (!embeddedJsonDirective.equals(other.embeddedJsonDirective))
            return false;
        if (embeddedStringDirective == null) {
            if (other.embeddedStringDirective != null)
                return false;
        } else if (!embeddedStringDirective.equals(other.embeddedStringDirective))
            return false;
        if (genCodeEndDirective == null) {
            if (other.genCodeEndDirective != null)
                return false;
        } else if (!genCodeEndDirective.equals(other.genCodeEndDirective))
            return false;
        if (genCodeStartDirective == null) {
            if (other.genCodeStartDirective != null)
                return false;
        } else if (!genCodeStartDirective.equals(other.genCodeStartDirective))
            return false;
        if (inlineGenCodeDirective == null) {
            if (other.inlineGenCodeDirective != null)
                return false;
        } else if (!inlineGenCodeDirective.equals(other.inlineGenCodeDirective))
            return false;
        if (nestedLevelEndMarker == null) {
            if (other.nestedLevelEndMarker != null)
                return false;
        } else if (!nestedLevelEndMarker.equals(other.nestedLevelEndMarker))
            return false;
        if (nestedLevelStartMarker == null) {
            if (other.nestedLevelStartMarker != null)
                return false;
        } else if (!nestedLevelStartMarker.equals(other.nestedLevelStartMarker))
            return false;
        if (skipCodeEndDirective == null) {
            if (other.skipCodeEndDirective != null)
                return false;
        } else if (!skipCodeEndDirective.equals(other.skipCodeEndDirective))
            return false;
        if (skipCodeStartDirective == null) {
            if (other.skipCodeStartDirective != null)
                return false;
        } else if (!skipCodeStartDirective.equals(other.skipCodeStartDirective))
            return false;
        if (sourceFileAugmentingCodes == null) {
            if (other.sourceFileAugmentingCodes != null)
                return false;
        } else if (!sourceFileAugmentingCodes.equals(other.sourceFileAugmentingCodes))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CodeGenerationRequest{augCodeDirective=" + augCodeDirective + ", embeddedJsonDirective="
                + embeddedJsonDirective + ", embeddedStringDirective=" + embeddedStringDirective
                + ", genCodeEndDirective=" + genCodeEndDirective + ", genCodeStartDirective=" + genCodeStartDirective
                + ", inlineGenCodeDirective=" + inlineGenCodeDirective + ", nestedLevelEndMarker="
                + nestedLevelEndMarker + ", nestedLevelStartMarker=" + nestedLevelStartMarker
                + ", skipCodeEndDirective=" + skipCodeEndDirective + ", skipCodeStartDirective="
                + skipCodeStartDirective + ", sourceFileAugmentingCodes=" + sourceFileAugmentingCodes + "}";
    }
}