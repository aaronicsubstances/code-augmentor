package com.aaronicsubstances.code.augmentor.models;

import java.util.List;

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

    private int index;
    private List<Block> blocks;
    private String commentSuffix;

    public AugmentingCode() {
    }

    public AugmentingCode(List<Block> blocks) {
        this.blocks = blocks;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((blocks == null) ? 0 : blocks.hashCode());
        result = prime * result + ((commentSuffix == null) ? 0 : commentSuffix.hashCode());
        result = prime * result + index;
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
        return true;
    }

    @Override
    public String toString() {
        return "AugmentingCode{blocks=" + blocks + ", commentSuffix=" + commentSuffix + 
                ", index=" + index + "}";
    }
}