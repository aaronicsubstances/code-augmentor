package com.aaronicsubstances.code.augmentor.core.tasks;

public class GenericTaskException extends RuntimeException {
    private static final long serialVersionUID = 5939044801925095816L;

    public GenericTaskException(String message) {
        super(message);
    }

    public GenericTaskException(String message, Throwable cause) {
        super(message, cause);
    }    
}