package com.aaronicsubstances.programmer.companion.ant.plugin.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.aaronicsubstances.programmer.companion.ant.plugin.persistence.ModifiedCsvReader;
import com.aaronicsubstances.programmer.companion.ant.plugin.persistence.ModifiedCsvWriter;
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

    public void serialize(Object serializer) throws Exception {
        if (serializer instanceof XMLStreamWriter) {
            XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
            xmlWriter.writeStartElement("augmenting_code");
            xmlWriter.writeAttribute("rel_path", relativePath);
            xmlWriter.writeAttribute("index_in_file", "" + indexInFile);
            xmlWriter.writeAttribute("index", "" + index);

            xmlWriter.writeStartElement("block_list");

            for (Block block : blocks) {
                xmlWriter.writeStartElement("block");
                xmlWriter.writeAttribute("stringify", "" + block.stringify);
                xmlWriter.writeCharacters(block.content);            
                xmlWriter.writeEndElement();
            }

            xmlWriter.writeEndElement(); // block_list
            xmlWriter.writeEndElement(); // augmenting_code
        }
        else {
            ModifiedCsvWriter qCsvWriter = (ModifiedCsvWriter) serializer;
            for (Block block : blocks) {
                Object[] record = new Object[]{
                    relativePath, index, indexInFile, block.stringify, block.content  
                };
                qCsvWriter.writeRecord(record);
            }
            qCsvWriter.writeSeparatorLine();
        }
    }

    public boolean deserialize(Object deserializer) throws Exception {
        blocks = new ArrayList<>();
        if (deserializer instanceof XmlEventReaderWrapper) {
            XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
            StartElement startElement = xmlReader.locateStartElement("augmenting_code");
            if (startElement == null) {
                return false;
            }
            index = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, "index");
            indexInFile = XmlEventReaderWrapper.requireAttributeValueAsInt(startElement, "index_in_file");
            relativePath = XmlEventReaderWrapper.requireAttributeValue(startElement, "rel_path");
            
            startElement = xmlReader.requireStartElement("block_list");

            while ((startElement = xmlReader.locateStartElement("block")) != null) {
                Block block = new Block();
                Attribute stringifyAtt = startElement.getAttributeByName(QName.valueOf("stringify"));
                if (stringifyAtt != null) {
                    block.stringify = Boolean.parseBoolean(stringifyAtt.getValue());
                }
                block.content = xmlReader.readElementValue();
                xmlReader.requireEndElement("block");

                blocks.add(block);
            }
            
            xmlReader.requireEndElement("block_list");
            xmlReader.requireEndElement("augmenting_code");

            return true;
        }
        else {
            Object[] readState = (Object[]) deserializer;
            ModifiedCsvReader qCsvReader = (ModifiedCsvReader) readState[0];

            // attempt to make use of last result.
            Object result = readState[1];
            if (result == null) {
                // Do a first search to locate first of blocks.
                while ((result = qCsvReader.read()) != null) {
                    if (result instanceof String[]) {
                        break;
                    }
                }
            }

            if (result == null) {
                return false;
            }

            // clear read state of last result.
            readState[1] = null;

            // Read until there's a change in any of augmenting code properties
            // after it is set.
            do {
                if (!(result instanceof String[])) {
                    continue;
                }
                String[] record = (String[]) result;
                Map<String, String> recordDict = qCsvReader.convertRecordToDict(record);
                String newRelativePath = recordDict.get("rel_path");
                if (relativePath != null && !relativePath.equals(newRelativePath)) {
                    readState[1] = result;
                    break;
                }
                relativePath = newRelativePath;

                try {
                    int newIndex = Integer.parseInt(recordDict.get("index"));
                    if (index != 0 && index != newIndex) {
                        readState[1] = result;
                        break;
                    }
                    index = newIndex;
                 }
                catch (NumberFormatException ex) {
                    throw qCsvReader.createAbortException("invalid index");
                }
                try {
                    int newIndexInFile = Integer.parseInt(recordDict.get("index_in_file"));
                    if (indexInFile != 0 && indexInFile != newIndexInFile) {
                        readState[1] = result;
                        break;
                    }
                    indexInFile = newIndexInFile;
                }
                catch (NumberFormatException ex) {
                    throw qCsvReader.createAbortException("invalid index_in_file");
                }
                
                boolean stringify = Boolean.parseBoolean(recordDict.get("stringify"));
                String blockContent = recordDict.get("block");
                Block block = new Block();
                block.stringify = stringify;
                block.content = blockContent;
                blocks.add(block);

            } while ((result = qCsvReader.read()) != null);

            return true;
        }
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