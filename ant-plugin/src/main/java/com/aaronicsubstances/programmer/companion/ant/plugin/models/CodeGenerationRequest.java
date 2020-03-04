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

/**
 * 
 */
public class CodeGenerationRequest {
    private List<AugmentingCode> augmentingCodeSnippets;

    public CodeGenerationRequest() {
    }

    public CodeGenerationRequest(List<AugmentingCode> augmentingCodeSnippets) {
        this.augmentingCodeSnippets = augmentingCodeSnippets;
    }

    public List<AugmentingCode> getAugmentingCodeSnippets() {
        return augmentingCodeSnippets;
    }

    public void setAugmentingCodeSnippets(List<AugmentingCode> augmentingCodeSnippets) {
        this.augmentingCodeSnippets = augmentingCodeSnippets;
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
        xmlWriter.writeStartElement("request");
        xmlWriter.writeStartElement("augmenting_code_list");
        return xmlWriter;
    }

    public void endSerialize(Object serializer) throws Exception {
        if (serializer != null) {
            XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
            try {
                xmlWriter.writeEndElement(); // augmenting_code_list
                xmlWriter.writeEndElement(); // request
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
        xmlReader.requireDocOpener("request");        
        xmlReader.requireStartElement("augmenting_code_list");

        augmentingCodeSnippets = new ArrayList<>();
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
        result = prime * result + ((augmentingCodeSnippets == null) ? 0 : augmentingCodeSnippets.hashCode());
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
        if (augmentingCodeSnippets == null) {
            if (other.augmentingCodeSnippets != null)
                return false;
        } else if (!augmentingCodeSnippets.equals(other.augmentingCodeSnippets))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CodeGenerationRequest{augmentingCodeSnippets=" + augmentingCodeSnippets + "}";
    }
}