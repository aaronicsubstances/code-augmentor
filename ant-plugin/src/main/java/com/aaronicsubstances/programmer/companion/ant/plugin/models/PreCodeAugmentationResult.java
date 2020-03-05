package com.aaronicsubstances.programmer.companion.ant.plugin.models;

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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;

import com.aaronicsubstances.programmer.companion.ant.plugin.persistence.XmlEventReaderWrapper;

/**
 * 
 */
public class PreCodeAugmentationResult {
    private String codeSnippetBlockStartDoubleSlash;
    private String codeSnippetBlockEndDoubleSlash;
    private String codeSnippetBlockStartSlashStar;
    private String codeSnippetBlockEndSlashStar;
    private List<SourceFileDescriptor> fileDescriptors;

    public PreCodeAugmentationResult() {
    }

    public PreCodeAugmentationResult(List<SourceFileDescriptor> fileDescriptors) {
        this.fileDescriptors = fileDescriptors;
    }

    public List<SourceFileDescriptor> getFileDescriptors() {
        return fileDescriptors;
    }

    public void setFileDescriptors(List<SourceFileDescriptor> fileDescriptors) {
        this.fileDescriptors = fileDescriptors;
    }

    public String getCodeSnippetBlockStartDoubleSlash() {
        return codeSnippetBlockStartDoubleSlash;
    }

    public void setCodeSnippetBlockStartDoubleSlash(String codeSnippetBlockStartDoubleSlash) {
        this.codeSnippetBlockStartDoubleSlash = codeSnippetBlockStartDoubleSlash;
    }

    public String getCodeSnippetBlockEndDoubleSlash() {
        return codeSnippetBlockEndDoubleSlash;
    }

    public void setCodeSnippetBlockEndDoubleSlash(String codeSnippetBlockEndDoubleSlash) {
        this.codeSnippetBlockEndDoubleSlash = codeSnippetBlockEndDoubleSlash;
    }

    public String getCodeSnippetBlockStartSlashStar() {
        return codeSnippetBlockStartSlashStar;
    }

    public void setCodeSnippetBlockStartSlashStar(String codeSnippetBlockStartSlashStar) {
        this.codeSnippetBlockStartSlashStar = codeSnippetBlockStartSlashStar;
    }

    public String getCodeSnippetBlockEndSlashStar() {
        return codeSnippetBlockEndSlashStar;
    }

    public void setCodeSnippetBlockEndSlashStar(String codeSnippetBlockEndSlashStar) {
        this.codeSnippetBlockEndSlashStar = codeSnippetBlockEndSlashStar;
    }

    public Object beginSerialize(File file) throws Exception {        
        OutputStreamWriter stream = new OutputStreamWriter(
            new FileOutputStream(file), StandardCharsets.UTF_8);
        Object serializer = beginSerialize(stream);
        return serializer;
    }

    Object beginSerialize(Writer stream) throws Exception {    
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlWriter = f.createXMLStreamWriter(stream);
        xmlWriter.writeStartDocument("utf-8", "1.0");
        xmlWriter.writeStartElement("result");
        xmlWriter.writeAttribute("code_snippet_block_start_double_slash", 
            codeSnippetBlockStartDoubleSlash);
        xmlWriter.writeAttribute("code_snippet_block_end_double_slash", 
            codeSnippetBlockEndDoubleSlash);
        xmlWriter.writeAttribute("code_snippet_block_start_slash_star", 
            codeSnippetBlockStartSlashStar);
        xmlWriter.writeAttribute("code_snippet_block_end_slash_star", 
            codeSnippetBlockEndSlashStar);

        xmlWriter.writeStartElement("file_list");
        return xmlWriter;
    }

    public void endSerialize(Object serializer) throws Exception {
        if (serializer != null) {
            XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
            try {
                xmlWriter.writeEndElement(); // file_list
                xmlWriter.writeEndElement(); // result
                xmlWriter.writeEndDocument();
            }
            finally {
                xmlWriter.close();
            }
        }
    }

    public Object beginDeserializer(File file) throws Exception {    
        InputStreamReader stream = new InputStreamReader(
            new FileInputStream(file), StandardCharsets.UTF_8);
        Object serializer = beginDeserialize(stream);
        return serializer;
    }

    Object beginDeserialize(Reader stream) throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XmlEventReaderWrapper xmlReader = new XmlEventReaderWrapper(inputFactory.createXMLEventReader(stream));

        StartElement rootElement = xmlReader.requireDocOpener("result");
        codeSnippetBlockStartDoubleSlash = XmlEventReaderWrapper.requireAttributeValue(rootElement,
            "code_snippet_block_start_double_slash");
        codeSnippetBlockEndDoubleSlash = XmlEventReaderWrapper.requireAttributeValue(rootElement,
            "code_snippet_block_end_double_slash");
        codeSnippetBlockStartSlashStar = XmlEventReaderWrapper.requireAttributeValue(rootElement,
            "code_snippet_block_start_slash_star");
        codeSnippetBlockEndSlashStar = XmlEventReaderWrapper.requireAttributeValue(rootElement, 
            "code_snippet_block_end_slash_star");

        xmlReader.requireStartElement("file_list");

        fileDescriptors = new ArrayList<>();
        return xmlReader;
    }

    public void endDeserialize(Object deserializer) throws Exception {
        if (deserializer != null) {
            XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
            xmlReader.close();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((codeSnippetBlockEndDoubleSlash == null) ? 0 : codeSnippetBlockEndDoubleSlash.hashCode());
        result = prime * result
                + ((codeSnippetBlockEndSlashStar == null) ? 0 : codeSnippetBlockEndSlashStar.hashCode());
        result = prime * result
                + ((codeSnippetBlockStartDoubleSlash == null) ? 0 : codeSnippetBlockStartDoubleSlash.hashCode());
        result = prime * result
                + ((codeSnippetBlockStartSlashStar == null) ? 0 : codeSnippetBlockStartSlashStar.hashCode());
        result = prime * result + ((fileDescriptors == null) ? 0 : fileDescriptors.hashCode());
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
        PreCodeAugmentationResult other = (PreCodeAugmentationResult) obj;
        if (codeSnippetBlockEndDoubleSlash == null) {
            if (other.codeSnippetBlockEndDoubleSlash != null)
                return false;
        } else if (!codeSnippetBlockEndDoubleSlash.equals(other.codeSnippetBlockEndDoubleSlash))
            return false;
        if (codeSnippetBlockEndSlashStar == null) {
            if (other.codeSnippetBlockEndSlashStar != null)
                return false;
        } else if (!codeSnippetBlockEndSlashStar.equals(other.codeSnippetBlockEndSlashStar))
            return false;
        if (codeSnippetBlockStartDoubleSlash == null) {
            if (other.codeSnippetBlockStartDoubleSlash != null)
                return false;
        } else if (!codeSnippetBlockStartDoubleSlash.equals(other.codeSnippetBlockStartDoubleSlash))
            return false;
        if (codeSnippetBlockStartSlashStar == null) {
            if (other.codeSnippetBlockStartSlashStar != null)
                return false;
        } else if (!codeSnippetBlockStartSlashStar.equals(other.codeSnippetBlockStartSlashStar))
            return false;
        if (fileDescriptors == null) {
            if (other.fileDescriptors != null)
                return false;
        } else if (!fileDescriptors.equals(other.fileDescriptors))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PreCodeAugmentationResult{codeSnippetBlockEndDoubleSlash=" + codeSnippetBlockEndDoubleSlash
                + ", codeSnippetBlockEndSlashStar=" + codeSnippetBlockEndSlashStar
                + ", codeSnippetBlockStartDoubleSlash=" + codeSnippetBlockStartDoubleSlash
                + ", codeSnippetBlockStartSlashStar=" + codeSnippetBlockStartSlashStar + ", fileDescriptors="
                + fileDescriptors + "}";
    }
}