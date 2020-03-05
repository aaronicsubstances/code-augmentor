package com.aaronicsubstances.programmer.companion.ant.plugin.models;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;

import com.aaronicsubstances.programmer.companion.ant.plugin.persistence.XmlEventReaderWrapper;

/**
 * 
 */
public class SourceFileDescriptor {
    private String dir;
    private String relativePath;
    private CodeSnippetDescriptor headerSnippet;
    private List<CodeSnippetDescriptor> bodySnippets;

    public SourceFileDescriptor() {
    }

    public SourceFileDescriptor(List<CodeSnippetDescriptor> bodySnippets) {
        this.bodySnippets = bodySnippets;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public CodeSnippetDescriptor getHeaderSnippet() {
        return headerSnippet;
    }

    public void setHeaderSnippet(CodeSnippetDescriptor headerSnippet) {
        this.headerSnippet = headerSnippet;
    }

    public List<CodeSnippetDescriptor> getBodySnippets() {
        return bodySnippets;
    }

    public void setBodySnippets(List<CodeSnippetDescriptor> bodySnippets) {
        this.bodySnippets = bodySnippets;
    }

    public void beginSerialize(Object serializer) throws Exception {    
        XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
        xmlWriter.writeStartElement("file");
        xmlWriter.writeAttribute("dir", dir);
        xmlWriter.writeAttribute("rel_path", relativePath);

        if (headerSnippet != null) {
            headerSnippet.serialize(xmlWriter, "header_snippet");
        }
        xmlWriter.writeStartElement("snippet_list");
    }

    public void endSerialize(Object serializer) throws Exception {        
        XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
        xmlWriter.writeEndElement(); // snippet_list
        xmlWriter.writeEndElement(); // file
        xmlWriter.flush();
    }

    public boolean beginDeserialize(Object deserializer) throws Exception {
        XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
        StartElement startElement = xmlReader.locateStartElement("file");
        if (startElement == null) {
            return false;
        }
        dir = XmlEventReaderWrapper.requireAttributeValue(startElement, "dir");
        relativePath = XmlEventReaderWrapper.requireAttributeValue(startElement, "rel_path");

        startElement = xmlReader.requireStartElement(new String[]{ "header_snippet", "snippet_list" });
        if ("header_snippet".equals(startElement.getName().getLocalPart())) {
            headerSnippet = new CodeSnippetDescriptor();
            headerSnippet.deserialize(xmlReader, startElement);
            xmlReader.requireEndElement("header_snippet");
            startElement = xmlReader.requireStartElement("snippet_list");
        }
        bodySnippets = new ArrayList<>();
        return true;
    }

    public void endDeserialize(Object deserializer) throws Exception {        
        XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
        xmlReader.requireEndElement("snippet_list");
        xmlReader.requireEndElement("file");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bodySnippets == null) ? 0 : bodySnippets.hashCode());
        result = prime * result + ((dir == null) ? 0 : dir.hashCode());
        result = prime * result + ((headerSnippet == null) ? 0 : headerSnippet.hashCode());
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
        SourceFileDescriptor other = (SourceFileDescriptor) obj;
        if (bodySnippets == null) {
            if (other.bodySnippets != null)
                return false;
        } else if (!bodySnippets.equals(other.bodySnippets))
            return false;
        if (dir == null) {
            if (other.dir != null)
                return false;
        } else if (!dir.equals(other.dir))
            return false;
        if (headerSnippet == null) {
            if (other.headerSnippet != null)
                return false;
        } else if (!headerSnippet.equals(other.headerSnippet))
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
        return "SourceFileDescriptor{bodySnippets=" + bodySnippets + ", dir=" + dir + ", headerSnippet="
                + headerSnippet + ", relativePath=" + relativePath + "}";
    }
}