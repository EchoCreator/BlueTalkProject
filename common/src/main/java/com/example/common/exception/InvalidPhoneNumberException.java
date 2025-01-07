package com.example.common.exception;

public class InvalidPhoneNumberException extends BaseException {
    public InvalidPhoneNumberException() {
    }

    public InvalidPhoneNumberException(String message) {
        super(message);
    }
}
