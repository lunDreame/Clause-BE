package com.clause.app.common;

import lombok.Getter;

@Getter
public class ClauseException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object details;

    public ClauseException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public ClauseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public ClauseException(ErrorCode errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ClauseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.details = null;
    }
}

