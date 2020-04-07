package com.aaronicsubstances.code.augmentor.core.parsing;

/**
 * Exception thrown during course of parsing operations.
 */
public class ParserException extends RuntimeException {
    private static final long serialVersionUID = 3489428574490L;

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}