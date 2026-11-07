package com.itheima.exception;

import com.itheima.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //业务异常：属于预期内的拦截（比如"该班级下有学生，不能删除"），用 warn 记录，避免污染 ERROR 日志
    @ExceptionHandler
    public Result handleBusinessException(BusinessException e){
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getMessage());
    }

    @ExceptionHandler
    public Result handleException(Exception e){
        log.error("异常信息 ", e);
        return Result.error("操作失败");
    }

    //接口不存在时不要再包装成"操作失败"，否则会把 404 掩盖成 200
    @ExceptionHandler
    public Result handleNoResourceFoundException(NoResourceFoundException e){
        log.warn("接口不存在：{} {}", e.getHttpMethod(), e.getResourcePath());
        return Result.error("接口不存在");
    }

    @ExceptionHandler
    public Result handlerDuplicateKeyException(DuplicateKeyException e){
        log.error("异常信息 ", e);
        String message = e.getMessage();
        int index = message.indexOf("Duplicate entry");
        String errMsg = message.substring(index);
        String[] arr = errMsg.split(" ");
        return Result.error(arr[2] + " 已存在");
    }




}
