package com.aaronicsubstances.programmer.companion.ant.plugin.models;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;

import com.aaronicsubstances.programmer.companion.ant.plugin.persistence.XmlEventReaderWrapper;

/**
 * 
 */
public class GeneratedCode {
    private String relativePath;
    private int index;
    private int indexInFile;
    private String headerContent;
    private String bodyContent;

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndexInFile() {
        return indexInFile;
    }

    public void setIndexInFile(int indexInFile) {
        this.indexInFile = indexInFile;
    }

    public String getHeaderContent() {
        return headerContent;
    }

    public void setHeaderContent(String headerContent) {
        this.headerContent = headerContent;
    }

    public String getBodyContent() {
        return bodyContent;
    }

    public void setBodyContent(String bodyContent) {
        this.bodyContent = bodyContent;
    }

	public void serialize(Object serializer) throws Exception {
        XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
        xmlWriter.writeStartElement("generated_code");
        xmlWriter.writeAttribute("rel_path", relativePath);
        xmlWriter.writeAttribute("index_in_file", "" + indexInFile);
        xmlWriter.writeAttribute("index", ""+ index);

        if (headerContent != null) {
            xmlWriter.writeStartElement("header");
            xmlWriter.writeCharacters(headerContent);
            xmlWriter.writeEndElement();
        }

        xmlWriter.writeStartElement("body");
        xmlWriter.writeCharacters(bodyContent);
        xmlWriter.writeEndElement();
        
        xmlWriter.writeEndElement();
        xmlWriter.flush();
    }

	public boolean deserialize(Object deserializer) throws Exception {
        XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
        StartElement startElement = xmlReader.locateStartElement("generated_code");
        if (startElement == null) {
            return false;
        }
        relativePath = XmlEventReaderWrapper.requireAttributeValue(startElement, "rel_path");
        indexInFile = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, "index_in_file");
        index = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, "index");

        startElement = xmlReader.requireStartElement(new String[]{ "header", "body" });
        if ("header".equals(startElement.getName().getLocalPart())) {
            headerContent = xmlReader.readElementValue();
            xmlReader.requireEndElement("header");
            startElement = xmlReader.requireStartElement("body");
        }

        bodyContent = xmlReader.readElementValue();
        xmlReader.requireEndElement("body");

        xmlReader.requireEndElement("generated_code");
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bodyContent == null) ? 0 : bodyContent.hashCode());
        result = prime * result + ((headerContent == null) ? 0 : headerContent.hashCode());
        result = prime * result + index;
        result = prime * result + indexInFile;
        result = prime * result + ((relativePath == null) ? 0 : relativePath.hashCode());
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
        GeneratedCode other = (GeneratedCode) obj;
        if (bodyContent == null) {
            if (other.bodyContent != null)
                return false;
        } else if (!bodyContent.equals(other.bodyContent))
            return false;
        if (headerContent == null) {
            if (other.headerContent != null)
                return false;
        } else if (!headerContent.equals(other.headerContent))
            return false;
        if (index != other.index)
            return false;
        if (indexInFile != other.indexInFile)
            return false;
        if (relativePath == null) {
            if (other.relativePath != null)
                return false;
        } else if (!relativePath.equals(other.relativePath))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "GeneratedCode{bodyContent=" + bodyContent + ", headerContent=" + headerContent + ", index=" + index
                + ", indexInFile=" + indexInFile + ", relativePath=" + relativePath + "}";
    }
}