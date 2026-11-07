package com.itheima.exception;

/**
 * 业务异常：用于抛出需要展示给用户的提示信息
 * 由 GlobalExceptionHandler 捕获，把 message 原样返回给前端
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

}
