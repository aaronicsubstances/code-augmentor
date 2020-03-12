package com.aaronicsubstances.programmer.companion.ant.plugin.models;

import java.util.Map;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;

import com.aaronicsubstances.programmer.companion.ant.plugin.persistence.ModifiedCsvReader;
import com.aaronicsubstances.programmer.companion.ant.plugin.persistence.ModifiedCsvWriter;
import com.aaronicsubstances.programmer.companion.ant.plugin.persistence.XmlEventReaderWrapper;

/**
 * 
 */
public class GeneratedCode {
    private String relativePath;
    private int index;
    private boolean error;
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

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
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
        if (serializer instanceof XMLStreamWriter) {
            XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
            xmlWriter.writeStartElement("generated_code");
            xmlWriter.writeAttribute("rel_path", relativePath);
            xmlWriter.writeAttribute("index", ""+ index);
            xmlWriter.writeAttribute("error", ""+ error);

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
        else {
            ModifiedCsvWriter qCsvWriter = (ModifiedCsvWriter) serializer;
            Object[] record = { relativePath, index, error, headerContent, bodyContent};
            qCsvWriter.writeRecord(record);
        }
    }

	public boolean deserialize(Object deserializer) throws Exception {
        if (deserializer instanceof XmlEventReaderWrapper) {
            XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
            StartElement startElement = xmlReader.locateStartElement("generated_code");
            if (startElement == null) {
                return false;
            }
            relativePath = XmlEventReaderWrapper.requireAttributeValue(startElement, "rel_path");
            index = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, "index");
            error = XmlEventReaderWrapper.requireAttributeValueAsBoolean(startElement, "error");

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
        else {
            ModifiedCsvReader qCsvReader = (ModifiedCsvReader) deserializer;
            Object result;
            while ((result = qCsvReader.read()) != null) {
                if (result instanceof String[]) {
                    break;
                }
            }
            if (result == null) {
                return false;
            }
            String[] record = (String[]) result;
            Map<String, String> recordDict = qCsvReader.convertRecordToDict(record);
            relativePath = qCsvReader.requireField(recordDict, "rel_path", false);
            headerContent = recordDict.get("header");
            if ("".equals(headerContent)) {
                headerContent = null;
            }
            bodyContent = qCsvReader.requireField(recordDict, "body", true);

            error = Boolean.parseBoolean(recordDict.get("is_error"));
            index = qCsvReader.requireIntField(recordDict, "index");

            return true;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bodyContent == null) ? 0 : bodyContent.hashCode());
        result = prime * result + (error ? 1231 : 1237);
        result = prime * result + ((headerContent == null) ? 0 : headerContent.hashCode());
        result = prime * result + index;
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
        if (error != other.error)
            return false;
        if (headerContent == null) {
            if (other.headerContent != null)
                return false;
        } else if (!headerContent.equals(other.headerContent))
            return false;
        if (index != other.index)
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
        return "GeneratedCode{bodyContent=" + bodyContent + ", error=" + error + ", headerContent=" + headerContent
                + ", index=" + index + ", relativePath=" + relativePath + "}";
    }
}