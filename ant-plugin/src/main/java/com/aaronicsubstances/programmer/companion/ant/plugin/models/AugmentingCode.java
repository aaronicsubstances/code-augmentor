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

        public Block() {
        }

        public Block(String content, boolean stringify) {
            this.content = content;
            this.stringify = stringify;
        }

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
    private List<Block> blocks;
    private String commentSuffix;

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

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public String getCommentSuffix() {
        return commentSuffix;
    }

    public void setCommentSuffix(String commentSuffix) {
        this.commentSuffix = commentSuffix;
    }

    public void serialize(Object serializer) throws Exception {
        if (serializer instanceof XMLStreamWriter) {
            XMLStreamWriter xmlWriter = (XMLStreamWriter) serializer;
            xmlWriter.writeStartElement("augmenting_code");
            xmlWriter.writeAttribute("rel_path", relativePath);
            xmlWriter.writeAttribute("index", "" + index);
            xmlWriter.writeAttribute("comment_suffix", commentSuffix);

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

            // Copy augmenting code properties once.
            String dupRelPath = relativePath;
            String dupCommentSuffix = commentSuffix;
            for (Block block : blocks) {
                Object[] record = new Object[]{
                    index, dupRelPath, dupCommentSuffix, block.stringify, block.content  
                };
                qCsvWriter.writeRecord(record);
                dupRelPath = "";
                dupCommentSuffix = "";
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
            relativePath = XmlEventReaderWrapper.requireAttributeValue(startElement, "rel_path");
            commentSuffix = XmlEventReaderWrapper.requireAttributeValue(startElement, "comment_suffix");
            
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

            // Read until there's a change in index property of augmenting code
            // after it is set.
            boolean augmentingCodePropertieSet = false;
            do {
                if (!(result instanceof String[])) {
                    continue;
                }

                String[] record = (String[]) result;
                Map<String, String> recordDict = qCsvReader.convertRecordToDict(record);
                
                int newIndex = qCsvReader.requireIntField(recordDict, "index");
                if (augmentingCodePropertieSet && index != newIndex) {
                    readState[1] = result;
                    break;
                }
                index = newIndex;

                if (!augmentingCodePropertieSet) {
                    relativePath = qCsvReader.requireField(recordDict, "rel_path", false);
                    commentSuffix = qCsvReader.requireField(recordDict, "comment_suffix", false);
                    augmentingCodePropertieSet = true;
                }
                
                boolean stringify = Boolean.parseBoolean(recordDict.get("stringify"));
                String blockContent = qCsvReader.requireField(recordDict, "block", true);
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
        result = prime * result + ((commentSuffix == null) ? 0 : commentSuffix.hashCode());
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
        AugmentingCode other = (AugmentingCode) obj;
        if (blocks == null) {
            if (other.blocks != null)
                return false;
        } else if (!blocks.equals(other.blocks))
            return false;
        if (commentSuffix == null) {
            if (other.commentSuffix != null)
                return false;
        } else if (!commentSuffix.equals(other.commentSuffix))
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
        return "AugmentingCode{blocks=" + blocks + ", commentSuffix=" + commentSuffix + ", index=" + index
                + ", relativePath=" + relativePath + "}";
    }
}