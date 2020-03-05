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

import com.aaronicsubstances.programmer.companion.ant.plugin.persistence.XmlEventReaderWrapper;

/**
 * 
 */
public class CodeGenerationResponse {
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

    public Object beginSerialize(File file) throws Exception {        
        OutputStreamWriter stream = new OutputStreamWriter(
            new FileOutputStream(file), StandardCharsets.UTF_8);
        Object serializer = beginSerialize(stream);
        return serializer;
    }

    Object beginSerialize(Writer stream) throws Exception {    
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlWriter = f.createXMLStreamWriter(stream);
        xmlWriter.writeStartDocument("UTF-8", "1.0");
        xmlWriter.writeStartElement("response");
        xmlWriter.writeStartElement("generated_code_list");
        return xmlWriter;
    }

    public void endSerialize(Object serializer) throws Exception {
        if (serializer != null) {
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
        xmlReader.requireDocOpener("response");        
        xmlReader.requireStartElement("generated_code_list");

        generatedCodeSnippets = new ArrayList<>();
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