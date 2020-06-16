package com.aaronicsubstances.code.augmentor.core.tasks;

/**
 * Indicates to plugins that exceptions of this class can be unwrapped
 * and recast into plugin-specific exception type to reduce stack trace
 * size involving exceptions and their descendant inner causes.
 */
public class GenericTaskException extends RuntimeException {
    private static final long serialVersionUID = 5939044801925095816L;

    /**
     * Internal API used in library for creating exceptions encountered during
     * Code Augmentor operations.
     * @param cause optional inner exception
     * @param message exception message
     * @param srcPath optional path to source file which was being processed when exception occured.
     * @param srcLineNumber optional line number in source file where exception occured
     * @param srcFileSnippet optional snippet of code in source file around location of exception occurence
     * @return exception instance of this class
     */
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

    /**
     * Gets line number associated with exception for use during testing.
     * @return exception line number.
     */
	public int getLineNumber() {
		return lineNumber;
    }
}