package com.aaronicsubstances.code.augmentor.models;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import com.aaronicsubstances.code.augmentor.persistence.XmlEventReaderWrapper;

public class CodeGenerationResponse {
    private List<SourceFileGeneratedCode> sourceFileGeneratedCodeList;

    public CodeGenerationResponse() {
    }

    public CodeGenerationResponse(List<SourceFileGeneratedCode> sourceFileGeneratedCodeList) {
        this.sourceFileGeneratedCodeList = sourceFileGeneratedCodeList;
    }

    public List<SourceFileGeneratedCode> getSourceFileGeneratedCodeList() {
        return sourceFileGeneratedCodeList;
    }

    public void setSourceFileGeneratedCodeList(List<SourceFileGeneratedCode> sourceFileGeneratedCodeList) {
        this.sourceFileGeneratedCodeList = sourceFileGeneratedCodeList;
    }

    public Object beginSerialize(File file, boolean useXml) throws Exception {
        FileOutputStream fout = new FileOutputStream(file);
        if (useXml) {
            OutputStreamWriter stream = new OutputStreamWriter(
                fout, StandardCharsets.UTF_8);
            XMLOutputFactory f = XMLOutputFactory.newInstance();
            XMLStreamWriter xmlWriter = f.createXMLStreamWriter(stream);
            xmlWriter.writeStartDocument("UTF-8", "1.0");
            xmlWriter.writeStartElement("response");
            xmlWriter.writeStartElement("file_list");
            return xmlWriter;
        }
        else {
            ZipOutputStream zip = new ZipOutputStream(fout, StandardCharsets.UTF_8);
            return zip;
        }
    }

    public void endSerialize(Object serializer) throws Exception {
        if (serializer == null) {
            return;
        }
        if (serializer instanceof XMLStreamWriter) {
            XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
            try {
                xmlWriter.writeEndElement(); // file_list
                xmlWriter.writeEndElement(); // response
                xmlWriter.writeEndDocument();
            }
            finally {
                xmlWriter.close();
            }
        }
        else {
            ZipOutputStream zip = (ZipOutputStream) serializer;
            zip.finish();
        }
    }

    public static Object beginDeserialize(File file, boolean useXml) throws Exception {
        FileInputStream fin = new FileInputStream(file);
        if (useXml) {
            InputStreamReader stream = new InputStreamReader(
                fin, StandardCharsets.UTF_8);
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XmlEventReaderWrapper xmlReader = new XmlEventReaderWrapper(inputFactory.createXMLEventReader(stream));
            xmlReader.requireDocOpener("response");        
            xmlReader.requireStartElement("file_list");
            return xmlReader;
        }
        else {
            ZipInputStream zip = new ZipInputStream(fin, StandardCharsets.UTF_8);
            return zip;
        }
    }

    public static void endDeserialize(Object deserializer) throws Exception {
        if (deserializer == null) {
            return;
        }
        if (deserializer instanceof XmlEventReaderWrapper) {
            XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
            xmlReader.close();
        }
        else {
            ZipInputStream zip = (ZipInputStream) deserializer;
            zip.close();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sourceFileGeneratedCodeList == null) ? 0 : sourceFileGeneratedCodeList.hashCode());
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
        CodeGenerationResponse other = (CodeGenerationResponse) obj;
        if (sourceFileGeneratedCodeList == null) {
            if (other.sourceFileGeneratedCodeList != null)
                return false;
        } else if (!sourceFileGeneratedCodeList.equals(other.sourceFileGeneratedCodeList))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CodeGenerationResponse{sourceFileGeneratedCodeList=" + sourceFileGeneratedCodeList + "}";
    }
}