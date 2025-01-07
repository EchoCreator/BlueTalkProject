package com.example.server.handler;

import com.example.common.exception.BaseException;
import com.example.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BaseException.class)
    public Result exceptionHandler(BaseException e) {
        return Result.error(e.getMessage());
    }
}
