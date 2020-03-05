package com.aaronicsubstances.programmer.companion.ant.plugin.models;

import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.aaronicsubstances.programmer.companion.ant.plugin.persistence.XmlEventReaderWrapper;

/**
 * 
 */
public class AugmentingCode {
    
    public static class Block {
        private String content;
        private boolean stringify;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public boolean isStringify() {
            return stringify;
        }

        public void setStringify(boolean stringify) {
            this.stringify = stringify;
        }

        public void serialize(Object serializer) throws Exception {    
            XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
            xmlWriter.writeStartElement("block");
            xmlWriter.writeAttribute("stringify", "" + stringify);
            xmlWriter.writeCharacters(content);            
            xmlWriter.writeEndElement();
            xmlWriter.flush();
        }

        public boolean deserialize(Object deserializer) throws Exception {    
            XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
            StartElement startElement = xmlReader.locateStartElement("block");
            if (startElement == null) {
                return false;
            }
            Attribute stringifyAtt = startElement.getAttributeByName(QName.valueOf("stringify"));
            if (stringifyAtt != null) {
                stringify = Boolean.parseBoolean(stringifyAtt.getValue());
            }
            content = xmlReader.readElementValue();
            xmlReader.requireEndElement("block");
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((content == null) ? 0 : content.hashCode());
            result = prime * result + (stringify ? 1231 : 1237);
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
            Block other = (Block) obj;
            if (content == null) {
                if (other.content != null)
                    return false;
            } else if (!content.equals(other.content))
                return false;
            if (stringify != other.stringify)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Block{content=" + content + ", stringify=" + stringify + "}";
        }
    }

    private String relativePath;
    private int index;
    private int indexInFile;
    private List<Block> blocks;

    public AugmentingCode() {
    }

    public AugmentingCode(List<Block> blocks) {
        this.blocks = blocks;
    }

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

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public void beginSerialize(Object serializer) throws Exception {    
        XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
        xmlWriter.writeStartElement("augmenting_code");
        xmlWriter.writeAttribute("rel_path", relativePath);
        xmlWriter.writeAttribute("index_in_file", "" + indexInFile);
        xmlWriter.writeAttribute("index", "" + index);

        xmlWriter.writeStartElement("block_list");
    }

    public void endSerialize(Object serializer) throws Exception {        
        XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
        xmlWriter.writeEndElement(); // block_list
        xmlWriter.writeEndElement(); // augmenting_code
        xmlWriter.flush();
    }

    public boolean beginDeserialize(Object deserializer) throws Exception {
        XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
        StartElement startElement = xmlReader.locateStartElement("augmenting_code");
        if (startElement == null) {
            return false;
        }
        index = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, "index");
        indexInFile = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, "index_in_file");
        relativePath = XmlEventReaderWrapper.requireAttributeValue(startElement, "rel_path");
        
        startElement = xmlReader.requireStartElement("block_list");

        return true;
    }

    public void endDeserialize(Object deserializer) throws Exception {        
        XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
        xmlReader.requireEndElement("block_list");
        xmlReader.requireEndElement("augmenting_code");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((blocks == null) ? 0 : blocks.hashCode());
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
        AugmentingCode other = (AugmentingCode) obj;
        if (blocks == null) {
            if (other.blocks != null)
                return false;
        } else if (!blocks.equals(other.blocks))
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
        return "AugmentingCode{blocks=" + blocks + ", index=" + index + ", indexInFile=" + indexInFile
                + ", relativePath=" + relativePath + "}";
    }
}