package com.aaronicsubstances.code.augmentor.models;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.aaronicsubstances.code.augmentor.models.AugmentingCode.Block;
import com.aaronicsubstances.code.augmentor.persistence.XmlEventReaderWrapper;

public class SourceFileAugmentingCode {
    private int fileIndex;
    private String relativePath;
    private List<AugmentingCode> augmentingCodeList;

    public SourceFileAugmentingCode() {

    }

    public SourceFileAugmentingCode(List<AugmentingCode> augmentingCodeList) {
        this.augmentingCodeList = augmentingCodeList;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public void setFileIndex(int fileIndex) {
        this.fileIndex = fileIndex;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public List<AugmentingCode> getAugmentingCodeList() {
        return augmentingCodeList;
    }

    public void setAugmentingCodeList(List<AugmentingCode> augmentingCodeList) {
        this.augmentingCodeList = augmentingCodeList;
    }

    public void serialize(Object serializer) throws Exception {
        /*if (serializer instanceof XMLStreamWriter)*/ {
            XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
            xmlWriter.writeStartElement("file");;
            xmlWriter.writeAttribute("file_index", "" + fileIndex);
            xmlWriter.writeAttribute("rel_path", relativePath);

            xmlWriter.writeStartElement("augmenting_code_list");

            for (AugmentingCode augCode : augmentingCodeList) {
                xmlWriter.writeStartElement("augmenting_code");
                xmlWriter.writeAttribute("index", "" + augCode.getIndex());
                xmlWriter.writeAttribute("comment_suffix", augCode.getCommentSuffix());

                xmlWriter.writeStartElement("block_list");

                for (Block block : augCode.getBlocks()) {
                    xmlWriter.writeStartElement("block");
                    xmlWriter.writeAttribute("stringify", "" + block.isStringify());
                    xmlWriter.writeCharacters(block.getContent());
                    xmlWriter.writeEndElement();
                }

                xmlWriter.writeEndElement(); // block_list
                xmlWriter.writeEndElement(); // augmenting_code
            }

            xmlWriter.writeEndElement(); // augmenting_code_list

            xmlWriter.writeEndElement(); // file
        }
    }

    public boolean deserialize(Object deserializer) throws Exception {
        augmentingCodeList = new ArrayList<>();
        /*if (deserializer instanceof XmlEventReaderWrapper)*/ {
            XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
            StartElement startElement = xmlReader.locateStartElement("file");
            if (startElement == null) {
                return false;
            }
            fileIndex = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, "file_index");
            relativePath = XmlEventReaderWrapper.requireAttributeValue(startElement, "rel_path");
            
            startElement = xmlReader.requireStartElement("augmenting_code_list");

            while ((startElement = xmlReader.locateStartElement("augmenting_code")) != null) {
                AugmentingCode augCode = new AugmentingCode(new ArrayList<>());
                augmentingCodeList.add(augCode);
                int index = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, "index");
                augCode.setIndex(index);
                String commentSuffix = XmlEventReaderWrapper.requireAttributeValue(startElement, "comment_suffix");
                augCode.setCommentSuffix(commentSuffix);

                startElement = xmlReader.requireStartElement("block_list");

                while ((startElement = xmlReader.locateStartElement("block")) != null) {
                    Block block = new Block();
                    augCode.getBlocks().add(block);
                    Attribute stringifyAtt = startElement.getAttributeByName(QName.valueOf("stringify"));
                    if (stringifyAtt != null) {
                        block.setStringify(Boolean.parseBoolean(stringifyAtt.getValue()));
                    }
                    block.setContent(xmlReader.readElementValue());
                    xmlReader.requireEndElement("block");

                }
                
                xmlReader.requireEndElement("block_list");
                xmlReader.requireEndElement("augmenting_code");
            }

            xmlReader.requireEndElement("augmenting_code_list");
            xmlReader.requireEndElement("file");

            return true;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((augmentingCodeList == null) ? 0 : augmentingCodeList.hashCode());
        result = prime * result + fileIndex;
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
        SourceFileAugmentingCode other = (SourceFileAugmentingCode) obj;
        if (augmentingCodeList == null) {
            if (other.augmentingCodeList != null)
                return false;
        } else if (!augmentingCodeList.equals(other.augmentingCodeList))
            return false;
        if (fileIndex != other.fileIndex)
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
        return "SourceFileAugmentingCode{augmentingCodeList=" + augmentingCodeList +
                ", fileIndex=" + fileIndex +
                ", relativePath=" + relativePath + "}";
    }
}