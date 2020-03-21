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
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import com.aaronicsubstances.code.augmentor.persistence.ModifiedCsvReader;
import com.aaronicsubstances.code.augmentor.persistence.ModifiedCsvWriter;
import com.aaronicsubstances.code.augmentor.persistence.XmlEventReaderWrapper;

/**
 * 
 */
public class CodeGenerationResponse {
    private static final String[] csvFields;

    static {
        csvFields = new String[]{ "rel_path", "index", "is_error",
            "header", "body" };
    }

    private List<GeneratedCode> generatedCodeSnippets;

    public CodeGenerationResponse() {
    }

    public CodeGenerationResponse(List<GeneratedCode> generatedCodeSnippets) {
        this.generatedCodeSnippets = generatedCodeSnippets;
    }

    public List<GeneratedCode> getGeneratedCodeSnippets() {
        return generatedCodeSnippets;
    }

    public void setGeneratedCodeSnippets(List<GeneratedCode> generatedCodeSnippets) {
        this.generatedCodeSnippets = generatedCodeSnippets;
    }

    public Object beginSerialize(File file, boolean useXml) throws Exception {
        OutputStreamWriter stream = new OutputStreamWriter(
            new FileOutputStream(file), StandardCharsets.UTF_8);
        Object serializer = beginSerialize(stream, useXml);
        return serializer;
    }

    public Object beginSerialize(Writer stream, boolean useXml) throws Exception {
        if (useXml) {
            XMLOutputFactory f = XMLOutputFactory.newInstance();
            XMLStreamWriter xmlWriter = f.createXMLStreamWriter(stream);
            xmlWriter.writeStartDocument("UTF-8", "1.0");
            xmlWriter.writeStartElement("response");
            xmlWriter.writeStartElement("generated_code_list");
            return xmlWriter;
        }
        else {
            ModifiedCsvWriter qCsvWriter = new ModifiedCsvWriter(stream);
            qCsvWriter.writeFields(csvFields);
            return qCsvWriter;
        }
    }

    public void endSerialize(Object serializer) throws Exception {
        if (serializer == null) {
            return;
        }
        if (serializer instanceof XMLStreamWriter) {
            XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
            try {
                xmlWriter.writeEndElement(); // generated_code_list
                xmlWriter.writeEndElement(); // response
                xmlWriter.writeEndDocument();
            }
            finally {
                xmlWriter.close();
            }
        }
        else {
            ModifiedCsvWriter quasiCsvWriter = (ModifiedCsvWriter) serializer;
            quasiCsvWriter.close();
        }
    }

    public Object beginDeserializer(File file, boolean useXml) throws Exception {    
        InputStreamReader stream = new InputStreamReader(
            new FileInputStream(file), StandardCharsets.UTF_8);
        Object serializer = beginDeserialize(stream, useXml);
        return serializer;
    }

    public Object beginDeserialize(Reader stream, boolean useXml) throws Exception {
        generatedCodeSnippets = new ArrayList<>();
        if (useXml) {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            XmlEventReaderWrapper xmlReader = new XmlEventReaderWrapper(inputFactory.createXMLEventReader(stream));
            xmlReader.requireDocOpener("response");        
            xmlReader.requireStartElement("generated_code_list");
            return xmlReader;
        }
        else {
            ModifiedCsvReader qCsvReader = new ModifiedCsvReader(stream);
            List<String> fields = qCsvReader.requireFieldsOpener();
            if (!fields.containsAll(Arrays.asList(csvFields))) {
                throw qCsvReader.createAbortException("At least one expected field is absent: " +
                    "expected " + Arrays.toString(csvFields) + " but found " + fields);
            }
            return qCsvReader;
        }
    }

    public void endDeserialize(Object deserializer) throws Exception {
        if (deserializer == null) {
            return;
        }
        if (deserializer instanceof XmlEventReaderWrapper) {
            XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
            xmlReader.close();
        }
        else {
            ModifiedCsvReader qCsvReader = (ModifiedCsvReader) deserializer;
            qCsvReader.close();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((generatedCodeSnippets == null) ? 0 : generatedCodeSnippets.hashCode());
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
        if (generatedCodeSnippets == null) {
            if (other.generatedCodeSnippets != null)
                return false;
        } else if (!generatedCodeSnippets.equals(other.generatedCodeSnippets))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CodeGenerationResponse{generatedCodeSnippets=" + generatedCodeSnippets + "}";
    }
}