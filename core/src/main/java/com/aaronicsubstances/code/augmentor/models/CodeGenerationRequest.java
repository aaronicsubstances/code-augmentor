package com.aaronicsubstances.code.augmentor.models;

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

import com.aaronicsubstances.code.augmentor.persistence.XmlEventReaderWrapper;

public class CodeGenerationRequest {
    private List<SourceFileAugmentingCode> sourceFileAugmentingCodeList;

    public CodeGenerationRequest() {
    }

    public CodeGenerationRequest(List<SourceFileAugmentingCode> sourceFileAugmentingCodeList) {
        this.sourceFileAugmentingCodeList = sourceFileAugmentingCodeList;
    }

    public List<SourceFileAugmentingCode> getSourceFileAugmentingCodeList() {
        return sourceFileAugmentingCodeList;
    }

    public void setSourceFileAugmentingCodeList(List<SourceFileAugmentingCode> sourceFileAugmentingCodeList) {
        this.sourceFileAugmentingCodeList = sourceFileAugmentingCodeList;
    }

    public Object beginSerialize(File file, boolean useXml) throws Exception {        
        OutputStreamWriter stream = new OutputStreamWriter(
            new FileOutputStream(file), StandardCharsets.UTF_8);
        Object serializer = beginSerialize(stream, useXml);
        return serializer;
    }

    public Object beginSerialize(Writer stream, boolean useXml) throws Exception {
        /*if (useXml)*/ {
            XMLOutputFactory f = XMLOutputFactory.newInstance();
            XMLStreamWriter xmlWriter = f.createXMLStreamWriter(stream);
            xmlWriter.writeStartDocument("utf-8", "1.0");
            xmlWriter.writeStartElement("request");
            xmlWriter.writeStartElement("file_list");
            return xmlWriter;
        }
    }

    public void endSerialize(Object serializer) throws Exception {
        if (serializer == null) {
            return;
        }
        /*if (serializer instanceof XMLStreamWriter)*/ {
            XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
            try {
                xmlWriter.writeEndElement(); // file_list
                xmlWriter.writeEndElement(); // request
                xmlWriter.writeEndDocument();
            }
            finally {
                xmlWriter.close();
            }
        }
    }

    public Object beginDeserializer(File file, boolean useXml) throws Exception {    
        InputStreamReader stream = new InputStreamReader(
            new FileInputStream(file), StandardCharsets.UTF_8);
        Object serializer = beginDeserialize(stream, useXml);
        return serializer;
    }

    @SuppressWarnings("resource")
    public Object beginDeserialize(Reader stream, boolean useXml) throws Exception {
        sourceFileAugmentingCodeList = new ArrayList<>();
        /*if (useXml)*/ {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XmlEventReaderWrapper xmlReader = new XmlEventReaderWrapper(
                inputFactory.createXMLEventReader(stream));
            xmlReader.requireDocOpener("request");        
            xmlReader.requireStartElement("file_list");
    
            return xmlReader;
        }
    }

    public void endDeserialize(Object deserializer) throws Exception {
        if (deserializer == null) {
            return;
        }
        /*if (deserializer instanceof XmlEventReaderWrapper)*/ {
            XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
            xmlReader.close();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sourceFileAugmentingCodeList == null) ? 0 : sourceFileAugmentingCodeList.hashCode());
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
        if (sourceFileAugmentingCodeList == null) {
            if (other.sourceFileAugmentingCodeList != null)
                return false;
        } else if (!sourceFileAugmentingCodeList.equals(other.sourceFileAugmentingCodeList))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CodeGenerationRequest{sourceFileAugmentingCodeList=" + sourceFileAugmentingCodeList + "}";
    }
}