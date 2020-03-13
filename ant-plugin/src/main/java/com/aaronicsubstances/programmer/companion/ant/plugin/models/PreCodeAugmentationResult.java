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
    private String genCodeStartSuffix;
    private String genCodeEndSuffix;
    private String tempDir;
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

    public String getGenCodeStartSuffix() {
        return genCodeStartSuffix;
    }

    public void setGenCodeStartSuffix(String genCodeStartSuffix) {
        this.genCodeStartSuffix = genCodeStartSuffix;
    }

    public String getGenCodeEndSuffix() {
        return genCodeEndSuffix;
    }

    public void setGenCodeEndSuffix(String genCodeEndSuffix) {
        this.genCodeEndSuffix = genCodeEndSuffix;
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public Object beginSerialize(File file) throws Exception {        
        OutputStreamWriter stream = new OutputStreamWriter(
            new FileOutputStream(file), StandardCharsets.UTF_8);
        Object serializer = beginSerialize(stream);
        return serializer;
    }

    public Object beginSerialize(Writer stream) throws Exception {    
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlWriter = f.createXMLStreamWriter(stream);
        xmlWriter.writeStartDocument("utf-8", "1.0");
        xmlWriter.writeStartElement("result");
        xmlWriter.writeAttribute("gen_code_start_suffix", 
            genCodeStartSuffix);
        xmlWriter.writeAttribute("gen_code_end_suffix", 
            genCodeEndSuffix);
        xmlWriter.writeAttribute("temp_dir", 
            tempDir);

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

    public Object beginDeserialize(Reader stream) throws Exception {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XmlEventReaderWrapper xmlReader = new XmlEventReaderWrapper(inputFactory.createXMLEventReader(stream));

        StartElement rootElement = xmlReader.requireDocOpener("result");
        genCodeStartSuffix = XmlEventReaderWrapper.requireAttributeValue(rootElement,
            "gen_code_start_suffix");
        genCodeEndSuffix = XmlEventReaderWrapper.requireAttributeValue(rootElement,
            "gen_code_end_suffix");
        tempDir = XmlEventReaderWrapper.requireAttributeValue(rootElement,
            "temp_dir");

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
        result = prime * result + ((fileDescriptors == null) ? 0 : fileDescriptors.hashCode());
        result = prime * result + ((genCodeEndSuffix == null) ? 0 : genCodeEndSuffix.hashCode());
        result = prime * result + ((genCodeStartSuffix == null) ? 0 : genCodeStartSuffix.hashCode());
        result = prime * result + ((tempDir == null) ? 0 : tempDir.hashCode());
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
        if (fileDescriptors == null) {
            if (other.fileDescriptors != null)
                return false;
        } else if (!fileDescriptors.equals(other.fileDescriptors))
            return false;
        if (genCodeEndSuffix == null) {
            if (other.genCodeEndSuffix != null)
                return false;
        } else if (!genCodeEndSuffix.equals(other.genCodeEndSuffix))
            return false;
        if (genCodeStartSuffix == null) {
            if (other.genCodeStartSuffix != null)
                return false;
        } else if (!genCodeStartSuffix.equals(other.genCodeStartSuffix))
            return false;
        if (tempDir == null) {
            if (other.tempDir != null)
                return false;
        } else if (!tempDir.equals(other.tempDir))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PreCodeAugmentationResult{fileDescriptors=" + fileDescriptors + ", genCodeEndSuffix="
                + genCodeEndSuffix + ", genCodeStartSuffix=" + genCodeStartSuffix + ", tempDir=" + tempDir + "}";
    }
}