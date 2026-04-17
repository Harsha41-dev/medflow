package com.medflow.exception;

public class SymptomPredictionServiceUnavailableException extends RuntimeException {

    public SymptomPredictionServiceUnavailableException(String message) {
        super(message);
    }

    public SymptomPredictionServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
