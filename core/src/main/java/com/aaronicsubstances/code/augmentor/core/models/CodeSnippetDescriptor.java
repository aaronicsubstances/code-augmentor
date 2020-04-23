package com.aaronicsubstances.code.augmentor.core.models;

public class CodeSnippetDescriptor {

    public static class AugmentingCodeDescriptor {
        private int id = 0;
        private int startPos = 0;
        private int endPos = 0;
        private String indent;
        private int lineNumber;
        private String lineSeparator;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
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

        public int getLineNumber() {
            return lineNumber;
        }
    
        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public String getLineSeparator() {
            return lineSeparator;
        }

        public void setLineSeparator(String lineSeparator) {
            this.lineSeparator = lineSeparator;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + endPos;
            result = prime * result + ((indent == null) ? 0 : indent.hashCode());
            result = prime * result + id;
            result = prime * result + lineNumber;
            result = prime * result + ((lineSeparator == null) ? 0 : lineSeparator.hashCode());
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
            if (endPos != other.endPos)
                return false;
            if (indent == null) {
                if (other.indent != null)
                    return false;
            } else if (!indent.equals(other.indent))
                return false;
            if (id != other.id)
                return false;
            if (lineNumber != other.lineNumber)
                return false;
            if (lineSeparator == null) {
                if (other.lineSeparator != null)
                    return false;
            } else if (!lineSeparator.equals(other.lineSeparator))
                return false;
            if (startPos != other.startPos)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "AugmentingCodeDescriptor{endPos=" + endPos + ", indent=" + indent + ", id=" + id
                    + ", lineNumber=" + lineNumber + ", lineSeparator=" + lineSeparator + ", startPos=" + startPos
                    + "}";
        }
    }

    public static class GeneratedCodeDescriptor {
        private int startDirectiveStartPos = 0;
        private int startDirectiveEndPos = 0;
        private int endDirectiveStartPos = 0;
        private int endDirectiveEndPos = 0;

        public GeneratedCodeDescriptor() {
        }

        public GeneratedCodeDescriptor(int startDirectiveStartPos, int startDirectiveEndPos,
                int endDirectiveStartPos, int endDirectiveEndPos) {
            this.startDirectiveStartPos = startDirectiveStartPos;
            this.startDirectiveEndPos = startDirectiveEndPos;
            this.endDirectiveStartPos = endDirectiveStartPos;
            this.endDirectiveEndPos = endDirectiveEndPos;
        }

        public int getStartDirectiveStartPos() {
            return startDirectiveStartPos;
        }

        public void setStartDirectiveStartPos(int startDirectiveStartPos) {
            this.startDirectiveStartPos = startDirectiveStartPos;
        }

        public int getStartDirectiveEndPos() {
            return startDirectiveEndPos;
        }

        public void setStartDirectiveEndPos(int startDirectiveEndPos) {
            this.startDirectiveEndPos = startDirectiveEndPos;
        }

        public int getEndDirectiveStartPos() {
            return endDirectiveStartPos;
        }

        public void setEndDirectiveStartPos(int endDirectiveStartPos) {
            this.endDirectiveStartPos = endDirectiveStartPos;
        }

        public int getEndDirectiveEndPos() {
            return endDirectiveEndPos;
        }

        public void setEndDirectiveEndPos(int endDirectiveEndPos) {
            this.endDirectiveEndPos = endDirectiveEndPos;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + endDirectiveEndPos;
            result = prime * result + endDirectiveStartPos;
            result = prime * result + startDirectiveEndPos;
            result = prime * result + startDirectiveStartPos;
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
            if (endDirectiveEndPos != other.endDirectiveEndPos)
                return false;
            if (endDirectiveStartPos != other.endDirectiveStartPos)
                return false;
            if (startDirectiveEndPos != other.startDirectiveEndPos)
                return false;
            if (startDirectiveStartPos != other.startDirectiveStartPos)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "GeneratedCodeDescriptor{endDirectiveEndPos=" + endDirectiveEndPos + ", endDirectiveStartPos="
                    + endDirectiveStartPos + ", startDirectiveEndPos=" + startDirectiveEndPos
                    + ", startDirectiveStartPos=" + startDirectiveStartPos + "}";
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