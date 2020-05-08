package com.aaronicsubstances.code.augmentor.core.models;

public class CodeSnippetChangeDescriptor {

    public static class ExactValue {
        private int length;
        private String prefix;
        private String updatedSection;
        private String suffix;
        private int updatedSectionOffset;

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getUpdatedSection() {
            return updatedSection;
        }

        public void setUpdatedSection(String updatedSection) {
            this.updatedSection = updatedSection;
        }

        public String getSuffix() {
            return suffix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }

        public int getUpdatedSectionOffset() {
            return updatedSectionOffset;
        }

        public void setUpdatedSectionOffset(int updatedSectionOffset) {
            this.updatedSectionOffset = updatedSectionOffset;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + length;
            result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
            result = prime * result + ((suffix == null) ? 0 : suffix.hashCode());
            result = prime * result + ((updatedSection == null) ? 0 : updatedSection.hashCode());
            result = prime * result + updatedSectionOffset;
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
            ExactValue other = (ExactValue) obj;
            if (length != other.length)
                return false;
            if (prefix == null) {
                if (other.prefix != null)
                    return false;
            } else if (!prefix.equals(other.prefix))
                return false;
            if (suffix == null) {
                if (other.suffix != null)
                    return false;
            } else if (!suffix.equals(other.suffix))
                return false;
            if (updatedSection == null) {
                if (other.updatedSection != null)
                    return false;
            } else if (!updatedSection.equals(other.updatedSection))
                return false;
            if (updatedSectionOffset != other.updatedSectionOffset)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "ExactValue{length=" + length + ", prefix=" + prefix + ", suffix=" + suffix + ", updatedSection="
                    + updatedSection + ", updatedSectionOffset=" + updatedSectionOffset + "}";
        }
    }

    private int id;
    private String type;
    private transient int charIndex;
    private int srcCharIndex;
    private int srcLineNumber;
    private int srcColumnNumber;
    private String currentSection;
    private int destCharIndex;
    private int destLineNumber;
    private int destColumnNumber;
    private ExactValue expectedExactValue;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCharIndex() {
        return charIndex;
    }

    public void setCharIndex(int charIndex) {
        this.charIndex = charIndex;
    }

    public int getSrcCharIndex() {
        return srcCharIndex;
    }

    public void setSrcCharIndex(int srcCharIndex) {
        this.srcCharIndex = srcCharIndex;
    }

    public int getSrcLineNumber() {
        return srcLineNumber;
    }

    public void setSrcLineNumber(int srcLineNumber) {
        this.srcLineNumber = srcLineNumber;
    }

    public int getSrcColumnNumber() {
        return srcColumnNumber;
    }

    public void setSrcColumnNumber(int srcColumnNumber) {
        this.srcColumnNumber = srcColumnNumber;
    }

    public String getCurrentSection() {
        return currentSection;
    }

    public void setCurrentSection(String currentSection) {
        this.currentSection = currentSection;
    }

    public int getDestCharIndex() {
        return destCharIndex;
    }

    public void setDestCharIndex(int destCharIndex) {
        this.destCharIndex = destCharIndex;
    }

    public int getDestLineNumber() {
        return destLineNumber;
    }

    public void setDestLineNumber(int destLineNumber) {
        this.destLineNumber = destLineNumber;
    }

    public int getDestColumnNumber() {
        return destColumnNumber;
    }

    public void setDestColumnNumber(int destColumnNumber) {
        this.destColumnNumber = destColumnNumber;
    }

    public ExactValue getExpectedExactValue() {
        return expectedExactValue;
    }

    public void setExpectedExactValue(ExactValue expectedExactValue) {
        this.expectedExactValue = expectedExactValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + charIndex;
        result = prime * result + ((currentSection == null) ? 0 : currentSection.hashCode());
        result = prime * result + destCharIndex;
        result = prime * result + destColumnNumber;
        result = prime * result + destLineNumber;
        result = prime * result + ((expectedExactValue == null) ? 0 : expectedExactValue.hashCode());
        result = prime * result + id;
        result = prime * result + srcCharIndex;
        result = prime * result + srcColumnNumber;
        result = prime * result + srcLineNumber;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        CodeSnippetChangeDescriptor other = (CodeSnippetChangeDescriptor) obj;
        if (charIndex != other.charIndex)
            return false;
        if (currentSection == null) {
            if (other.currentSection != null)
                return false;
        } else if (!currentSection.equals(other.currentSection))
            return false;
        if (destCharIndex != other.destCharIndex)
            return false;
        if (destColumnNumber != other.destColumnNumber)
            return false;
        if (destLineNumber != other.destLineNumber)
            return false;
        if (expectedExactValue == null) {
            if (other.expectedExactValue != null)
                return false;
        } else if (!expectedExactValue.equals(other.expectedExactValue))
            return false;
        if (id != other.id)
            return false;
        if (srcCharIndex != other.srcCharIndex)
            return false;
        if (srcColumnNumber != other.srcColumnNumber)
            return false;
        if (srcLineNumber != other.srcLineNumber)
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CodeSnippetChangeDescriptor{charIndex=" + charIndex + ", currentSection=" + currentSection
                + ", destCharIndex=" + destCharIndex + ", destColumnNumber=" + destColumnNumber + ", destLineNumber="
                + destLineNumber + ", expectedExactValue=" + expectedExactValue + ", id=" + id + ", srcCharIndex="
                + srcCharIndex + ", srcColumnNumber=" + srcColumnNumber + ", srcLineNumber=" + srcLineNumber + ", type="
                + type + "}";
    }
}