package com.aaronicsubstances.programmer.companion.ant.plugin.models;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import com.aaronicsubstances.programmer.companion.ant.plugin.persistence.XmlEventReaderWrapper;

/**
 * 
 */
public class CodeSnippetDescriptor {

    public static class AugmentingCodeDescriptor {
        private int index = 0;
        private int startPos = 0;
        private int endPos = 0;
        private String indent;
        private boolean annotatedWithSlashStar;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getStartPos() {
            return startPos;
        }

        public void setStartPos(int startPos) {
            this.startPos = startPos;
        }

        public int getEndPos() {
            return endPos;
        }

        public void setEndPos(int endPos) {
            this.endPos = endPos;
        }

        public String getIndent() {
            return indent;
        }

        public void setIndent(String indent) {
            this.indent = indent;
        }

        public boolean isAnnotatedWithSlashStar() {
            return annotatedWithSlashStar;
        }

        public void setAnnotatedWithSlashStar(boolean annotatedWithSlashStar) {
            this.annotatedWithSlashStar = annotatedWithSlashStar;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (annotatedWithSlashStar ? 1231 : 1237);
            result = prime * result + endPos;
            result = prime * result + ((indent == null) ? 0 : indent.hashCode());
            result = prime * result + index;
            result = prime * result + startPos;
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
            AugmentingCodeDescriptor other = (AugmentingCodeDescriptor) obj;
            if (annotatedWithSlashStar != other.annotatedWithSlashStar)
                return false;
            if (endPos != other.endPos)
                return false;
            if (indent == null) {
                if (other.indent != null)
                    return false;
            } else if (!indent.equals(other.indent))
                return false;
            if (index != other.index)
                return false;
            if (startPos != other.startPos)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "AugmentingCodeDescriptor{annotatedWithSlashStar=" + annotatedWithSlashStar + ", endPos=" + endPos
                    + ", indent=" + indent + ", index=" + index + ", startPos="
                    + startPos + "}";
        }
    }

    public static class GeneratedCodeDescriptor {
        private int startPos = 0;
        private int endPos = 0;

        public GeneratedCodeDescriptor() {
		}

        public GeneratedCodeDescriptor(int startPos, int endPos) {
            this.startPos = startPos;
            this.endPos = endPos;
		}

		public int getStartPos() {
            return startPos;
        }

        public void setStartPos(int startPos) {
            this.startPos = startPos;
        }

        public int getEndPos() {
            return endPos;
        }

        public void setEndPos(int endPos) {
            this.endPos = endPos;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + endPos;
            result = prime * result + startPos;
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
            GeneratedCodeDescriptor other = (GeneratedCodeDescriptor) obj;
            if (endPos != other.endPos)
                return false;
            if (startPos != other.startPos)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "GeneratedCodeDescriptor{endPos=" + endPos + ", startPos=" + startPos + "}";
        }
    }
    
    private AugmentingCodeDescriptor augmentingCodeDescriptor;
    private GeneratedCodeDescriptor generatedCodeDescriptor;

    public CodeSnippetDescriptor() {

    }

    public CodeSnippetDescriptor(AugmentingCodeDescriptor augmentingCodeDescriptor,
            GeneratedCodeDescriptor generatedCodeDescriptor) {
        this.augmentingCodeDescriptor = augmentingCodeDescriptor;
        this.generatedCodeDescriptor = generatedCodeDescriptor;
    }

    public AugmentingCodeDescriptor getAugmentingCodeDescriptor() {
        return augmentingCodeDescriptor;
    }

    public void setAugmentingCodeDescriptor(AugmentingCodeDescriptor augmentingCodeDescriptor) {
        this.augmentingCodeDescriptor = augmentingCodeDescriptor;
    }

    public GeneratedCodeDescriptor getGeneratedCodeDescriptor() {
        return generatedCodeDescriptor;
    }

    public void setGeneratedCodeDescriptor(GeneratedCodeDescriptor generatedCodeDescriptor) {
        this.generatedCodeDescriptor = generatedCodeDescriptor;
    }

	public void serialize(Object serializer) throws Exception {    
        serialize((XMLStreamWriter) serializer, "snippet");
	}

	void serialize(XMLStreamWriter xmlWriter, String elementName) throws Exception {
        xmlWriter.writeStartElement(elementName);

        if (generatedCodeDescriptor != null) {
            xmlWriter.writeStartElement("generated_code_descriptor");
            xmlWriter.writeAttribute("start_pos", 
                "" + generatedCodeDescriptor.getStartPos());
            xmlWriter.writeAttribute("end_pos", 
                "" + generatedCodeDescriptor.getEndPos());
            xmlWriter.writeEndElement();
        }

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
        xmlWriter.flush();
    }
    
    public boolean deserialize(Object deserializer) throws Exception {
        XmlEventReaderWrapper xmlReader = (XmlEventReaderWrapper) deserializer;
        StartElement startElement = xmlReader.locateStartElement("snippet");
        if (startElement == null) {
            return false;
        }
        deserialize(xmlReader, startElement);
        xmlReader.requireEndElement("snippet");
        return true;
    }

    void deserialize(XmlEventReaderWrapper xmlReader, StartElement startElement) 
            throws Exception {
        int startPos, endPos;
        startElement = xmlReader.requireStartElement( 
            new String[]{ "generated_code_descriptor", "augmenting_code_descriptor" });
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

        augmentingCodeDescriptor = new AugmentingCodeDescriptor();
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
	}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((augmentingCodeDescriptor == null) ? 0 : augmentingCodeDescriptor.hashCode());
        result = prime * result + ((generatedCodeDescriptor == null) ? 0 : generatedCodeDescriptor.hashCode());
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
        CodeSnippetDescriptor other = (CodeSnippetDescriptor) obj;
        if (augmentingCodeDescriptor == null) {
            if (other.augmentingCodeDescriptor != null)
                return false;
        } else if (!augmentingCodeDescriptor.equals(other.augmentingCodeDescriptor))
            return false;
        if (generatedCodeDescriptor == null) {
            if (other.generatedCodeDescriptor != null)
                return false;
        } else if (!generatedCodeDescriptor.equals(other.generatedCodeDescriptor))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CodeSnippetDescriptor{augmentingCodeDescriptor=" + augmentingCodeDescriptor
                + ", generatedCodeDescriptor=" + generatedCodeDescriptor + "}";
    }
}