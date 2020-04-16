package com.aaronicsubstances.code.augmentor.core.tasks;

public class GenericTaskException extends RuntimeException {
    private static final long serialVersionUID = 5939044801925095816L;

    public static GenericTaskException create(Throwable cause, String message, 
            String srcPath, int srcLineNumber, String srcFileSnippet) {
        StringBuilder fullMessage = new StringBuilder();
        if (srcPath != null) {
            fullMessage.append("in ").append(srcPath);
        }
        if (srcLineNumber > 0) {
            if (fullMessage.length() > 0) {
                fullMessage.append(" ");
            }
            fullMessage.append("at line ").append(srcLineNumber);
        }
        if (fullMessage.length() > 0) {
            fullMessage.append(": ");   
        }
        fullMessage.append(message);
        if (srcFileSnippet != null) {
            fullMessage.append("\n\n").append(srcFileSnippet);
        }
        return new GenericTaskException(srcLineNumber, fullMessage.toString(), cause);
    }

    private final int lineNumber;

    public GenericTaskException(int lineNumber, String message) {
        super(message);
        this.lineNumber = lineNumber;
    }

    public GenericTaskException(int lineNumber, String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = lineNumber;
    }

	public int getLineNumber() {
		return lineNumber;
	}
}