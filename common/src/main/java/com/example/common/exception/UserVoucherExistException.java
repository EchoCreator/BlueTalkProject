package com.example.common.exception;

public class UserVoucherExistException extends BaseException {
    public UserVoucherExistException() {
    }

    public UserVoucherExistException(String message) {
        super(message);
    }
}
