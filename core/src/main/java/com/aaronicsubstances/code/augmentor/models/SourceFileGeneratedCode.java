package com.aaronicsubstances.code.augmentor.models;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.aaronicsubstances.code.augmentor.persistence.XmlEventReaderWrapper;

public class SourceFileGeneratedCode {
    private int fileIndex;
    private List<GeneratedCode> generatedCodeList;

    public SourceFileGeneratedCode() {

    }

    public SourceFileGeneratedCode(List<GeneratedCode> generatedCodeList) {
        this.generatedCodeList = generatedCodeList;
    }
    
    public int getFileIndex() {
        return fileIndex;
    }

    public void setFileIndex(int fileIndex) {
        this.fileIndex = fileIndex;
    }

    public List<GeneratedCode> getGeneratedCodeList() {
        return generatedCodeList;
    }

    public void setGeneratedCodeList(List<GeneratedCode> generatedCodeList) {
        this.generatedCodeList = generatedCodeList;
    }

	public void serialize(Object serializer) throws Exception {
        /*if (serializer instanceof XMLStreamWriter)*/ {
            XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
            xmlWriter.writeStartElement("file");
            xmlWriter.writeAttribute("file_index", "" + fileIndex);

            xmlWriter.writeStartElement("generated_code_list");

            for (GeneratedCode genCode : generatedCodeList) {
                xmlWriter.writeStartElement("generated_code");
                xmlWriter.writeAttribute("index", "" + genCode.getIndex());
                xmlWriter.writeAttribute("error", "" + genCode.isError());

                if (genCode.getHeaderContent() != null) {
                    xmlWriter.writeStartElement("header");
                    xmlWriter.writeCharacters(genCode.getHeaderContent());
                    xmlWriter.writeEndElement();
                }

                xmlWriter.writeStartElement("body");
                xmlWriter.writeCharacters(genCode.getBodyContent());
                xmlWriter.writeEndElement();
                
                xmlWriter.writeEndElement();
            }

            xmlWriter.writeEndElement(); // generated_code_list
            xmlWriter.writeEndElement(); // file

            xmlWriter.flush();
        }
    }

	public boolean deserialize(Object deserializer) throws Exception {
        generatedCodeList = new ArrayList<>();
        /*if (deserializer instanceof XmlEventReaderWrapper)*/ {
            XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
            StartElement startElement = xmlReader.locateStartElement("file");
            if (startElement == null) {
                return false;
            }
            fileIndex = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, "file_index");

            xmlReader.requireStartElement("generated_code_list");

            while ((startElement = xmlReader.locateStartElement("generated_code")) != null) {
                GeneratedCode genCode = new GeneratedCode();
                generatedCodeList.add(genCode);

                int index = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, "index");
                genCode.setIndex(index);
                Attribute att = startElement.getAttributeByName(QName.valueOf("error"));
                if (att != null) {
                    genCode.setError(Boolean.parseBoolean(att.getValue()));
                }

                startElement = xmlReader.requireStartElement(new String[]{ "header", "body" });
                if ("header".equals(startElement.getName().getLocalPart())) {
                    String headerContent = xmlReader.readElementValue();
                    genCode.setHeaderContent(headerContent);
                    xmlReader.requireEndElement("header");
                    startElement = xmlReader.requireStartElement("body");
                }

                String bodyContent = xmlReader.readElementValue();
                genCode.setBodyContent(bodyContent);
                xmlReader.requireEndElement("body");

                xmlReader.requireEndElement("generated_code");
            }

            xmlReader.requireEndElement("generated_code_list");
            xmlReader.requireEndElement("file");

            return true;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + fileIndex;
        result = prime * result + ((generatedCodeList == null) ? 0 : generatedCodeList.hashCode());
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
        SourceFileGeneratedCode other = (SourceFileGeneratedCode) obj;
        if (fileIndex != other.fileIndex)
            return false;
        if (generatedCodeList == null) {
            if (other.generatedCodeList != null)
                return false;
        } else if (!generatedCodeList.equals(other.generatedCodeList))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SourceFileGeneratedCode{fileIndex=" + fileIndex 
            + ", generatedCodeList=" + generatedCodeList + "}";
    }
}