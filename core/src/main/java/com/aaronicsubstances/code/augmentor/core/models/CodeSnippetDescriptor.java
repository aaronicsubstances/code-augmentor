package com.aaronicsubstances.code.augmentor.core.models;

import com.google.gson.annotations.SerializedName;

public class CodeSnippetDescriptor {

    public static class AugmentingCodeDescriptor {
        @SerializedName("index")
        private int index = 0;
        @SerializedName("start_pos")
        private int startPos = 0;
        @SerializedName("end_pos")
        private int endPos = 0;
        @SerializedName("indent")
        private String indent;
        @SerializedName("has_newline_ending")
        private boolean hasNewlineEnding;

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

		public boolean getHasNewlineEnding() {
			return hasNewlineEnding;
		}

		public void setHasNewlineEnding(boolean hasNewlineEnding) {
			this.hasNewlineEnding = hasNewlineEnding;
		}

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + endPos;
            result = prime * result + (hasNewlineEnding ? 1231 : 1237);
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
            if (endPos != other.endPos)
                return false;
            if (hasNewlineEnding != other.hasNewlineEnding)
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
            return "AugmentingCodeDescriptor{endPos=" + endPos
                    + ", hasNewlineEnding=" + hasNewlineEnding + ", indent=" + indent + ", index=" + index
                    + ", startPos=" + startPos + "}";
        }
    }

    public static class GeneratedCodeDescriptor {
        @SerializedName("start_pos")
        private int startPos = 0;
        @SerializedName("end_pos")
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
    
    @SerializedName("augmenting_code")
    private AugmentingCodeDescriptor augmentingCodeDescriptor;
    @SerializedName("generated_code")
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