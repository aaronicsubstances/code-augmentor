package com.aaronicsubstances.programmer.companion.ant.plugin.models;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.aaronicsubstances.programmer.companion.ant.plugin.models.CodeSnippetDescriptor.AugmentingCodeDescriptor;
import com.aaronicsubstances.programmer.companion.ant.plugin.models.CodeSnippetDescriptor.GeneratedCodeDescriptor;
import com.aaronicsubstances.programmer.companion.ant.plugin.persistence.XmlEventReaderWrapper;

public class SourceFileDescriptor {
    private String dir;
    private String relativePath;
    private List<String> importStatements;
    private CodeSnippetDescriptor headerSnippet;
    private List<CodeSnippetDescriptor> bodySnippets;
    private String contentHash;

    public SourceFileDescriptor() {
    }

    public SourceFileDescriptor(List<String> importStatements, 
            List<CodeSnippetDescriptor> bodySnippets) {
        this.importStatements = importStatements;
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

    public List<String> getImportStatements() {
        return importStatements;
    }

    public void setImportStatements(List<String> importStatements) {
        this.importStatements = importStatements;
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

	public String getContentHash() {
        return contentHash;
    }

	public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public void serialize(Object serializer) throws Exception {    
        XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
        xmlWriter.writeStartElement("file");
        xmlWriter.writeAttribute("dir", dir);
        xmlWriter.writeAttribute("rel_path", relativePath);
        xmlWriter.writeAttribute("hash", contentHash);

        xmlWriter.writeStartElement("import_list");
        for (String importStatement : importStatements) {
            xmlWriter.writeStartElement("import");
            xmlWriter.writeCharacters(importStatement);            
            xmlWriter.writeEndElement();
        }
        xmlWriter.writeEndElement(); // import_list

        if (headerSnippet != null) {
            serializeCodeSnippetDescriptor(xmlWriter, headerSnippet, "header_snippet");
        }
        xmlWriter.writeStartElement("snippet_list");

        for (CodeSnippetDescriptor snippet : bodySnippets) {
            serializeCodeSnippetDescriptor(xmlWriter, snippet, "snippet");
        }
               
        xmlWriter.writeEndElement(); // snippet_list
        xmlWriter.writeEndElement(); // file
        xmlWriter.flush();
    }

    private void serializeCodeSnippetDescriptor(XMLStreamWriter xmlWriter, 
            CodeSnippetDescriptor snippet, String elementName) throws Exception {
        xmlWriter.writeStartElement(elementName);

        GeneratedCodeDescriptor generatedCodeDescriptor = snippet.getGeneratedCodeDescriptor();
        if (generatedCodeDescriptor != null) {
            xmlWriter.writeStartElement("generated_code_descriptor");
            xmlWriter.writeAttribute("start_pos", 
                "" + generatedCodeDescriptor.getStartPos());
            xmlWriter.writeAttribute("end_pos", 
                "" + generatedCodeDescriptor.getEndPos());
            xmlWriter.writeEndElement();
        }

        AugmentingCodeDescriptor augmentingCodeDescriptor = snippet.getAugmentingCodeDescriptor();
        xmlWriter.writeStartElement("augmenting_code_descriptor");
        xmlWriter.writeAttribute("index", 
            "" + augmentingCodeDescriptor.getIndex());
        xmlWriter.writeAttribute("start_pos", 
            "" + augmentingCodeDescriptor.getStartPos());
        xmlWriter.writeAttribute("end_pos", 
            "" + augmentingCodeDescriptor.getEndPos());
        if (augmentingCodeDescriptor.getIndent() != null) {
            xmlWriter.writeAttribute("indent", augmentingCodeDescriptor.getIndent());
        }
        xmlWriter.writeAttribute("is_slash_star", 
            "" + augmentingCodeDescriptor.isAnnotatedWithSlashStar());
        xmlWriter.writeEndElement();

        xmlWriter.writeEndElement();
    }

    public boolean deserialize(Object deserializer) throws Exception {
        XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
        StartElement startElement = xmlReader.locateStartElement("file");
        if (startElement == null) {
            return false;
        }
        dir = XmlEventReaderWrapper.requireAttributeValue(startElement, "dir");
        relativePath = XmlEventReaderWrapper.requireAttributeValue(startElement, "rel_path");
        contentHash = XmlEventReaderWrapper.requireAttributeValue(startElement, "hash");
        
        importStatements = new ArrayList<>();
        startElement = xmlReader.requireStartElement("import_list");
        while ((startElement = xmlReader.locateStartElement("import")) != null) {
            String importStatement = xmlReader.readElementValue();
            xmlReader.requireEndElement("import");

            importStatements.add(importStatement);
        }
        xmlReader.requireEndElement("import_list");

        startElement = xmlReader.requireStartElement(new String[]{ "header_snippet", "snippet_list" });
        if ("header_snippet".equals(startElement.getName().getLocalPart())) {
            headerSnippet = deserialize(xmlReader, startElement);
            xmlReader.requireEndElement("header_snippet");
            startElement = xmlReader.requireStartElement("snippet_list");
        }

        bodySnippets = new ArrayList<>();
        while ((startElement = xmlReader.locateStartElement("snippet")) != null) {
            CodeSnippetDescriptor snippet = deserialize(xmlReader, startElement);
            bodySnippets.add(snippet);
            xmlReader.requireEndElement("snippet");
        }
            
        xmlReader.requireEndElement("snippet_list");
        xmlReader.requireEndElement("file");

        return true;
    }
    
    private CodeSnippetDescriptor deserialize(XmlEventReaderWrapper xmlReader, StartElement startElement) 
            throws Exception {
        int startPos, endPos;
        startElement = xmlReader.requireStartElement( 
            new String[]{ "generated_code_descriptor", "augmenting_code_descriptor" });
        GeneratedCodeDescriptor generatedCodeDescriptor = null;
        if ("generated_code_descriptor".equals(startElement.getName().getLocalPart())) {
            generatedCodeDescriptor = new GeneratedCodeDescriptor();            
            startPos = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, 
                "start_pos");
            generatedCodeDescriptor.setStartPos(startPos);
            endPos = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement,
                "end_pos");
            generatedCodeDescriptor.setEndPos(endPos);
            xmlReader.requireEndElement("generated_code_descriptor");

            startElement = xmlReader.requireStartElement( 
                "augmenting_code_descriptor");
        }

        AugmentingCodeDescriptor augmentingCodeDescriptor = new AugmentingCodeDescriptor();
        int index = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, 
            "index");
        augmentingCodeDescriptor.setIndex(index);
        startPos = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, 
            "start_pos");
        augmentingCodeDescriptor.setStartPos(startPos);
        endPos = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement,
            "end_pos");
        augmentingCodeDescriptor.setEndPos(endPos);
        Attribute att = startElement.getAttributeByName(QName.valueOf("indent"));
        if (att != null) {
            augmentingCodeDescriptor.setIndent(att.getValue());
        }
        boolean isSlashStar = XmlEventReaderWrapper.requireAttributeValueAsBoolean(startElement,
            "is_slash_star");
        augmentingCodeDescriptor.setAnnotatedWithSlashStar(isSlashStar);
        
        xmlReader.requireEndElement("augmenting_code_descriptor");

        return new CodeSnippetDescriptor(augmentingCodeDescriptor, generatedCodeDescriptor);
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bodySnippets == null) ? 0 : bodySnippets.hashCode());
        result = prime * result + ((contentHash == null) ? 0 : contentHash.hashCode());
        result = prime * result + ((dir == null) ? 0 : dir.hashCode());
        result = prime * result + ((headerSnippet == null) ? 0 : headerSnippet.hashCode());
        result = prime * result + ((importStatements == null) ? 0 : importStatements.hashCode());
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
        if (contentHash == null) {
            if (other.contentHash != null)
                return false;
        } else if (!contentHash.equals(other.contentHash))
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
        if (importStatements == null) {
            if (other.importStatements != null)
                return false;
        } else if (!importStatements.equals(other.importStatements))
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
        return "SourceFileDescriptor{bodySnippets=" + bodySnippets + ", contentHash=" + contentHash + ", dir=" + dir
                + ", headerSnippet=" + headerSnippet + ", importStatements=" + importStatements + ", relativePath="
                + relativePath + "}";
    }
}