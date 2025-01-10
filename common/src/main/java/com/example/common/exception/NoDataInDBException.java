package com.example.common.exception;

public class NoDataInDBException extends BaseException {
    public NoDataInDBException() {
    }

    public NoDataInDBException(String message) {
        super(message);
    }
}
