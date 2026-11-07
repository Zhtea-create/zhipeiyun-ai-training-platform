package com.itheima.aspect;

import com.itheima.annotation.Log;
import com.itheima.mapper.OperateLogMapper;
import com.itheima.pojo.OperateLog;
import com.itheima.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

/**
 * 操作日志切面：拦截所有标注 @Log 的 Controller 方法。
 * 日志写入失败不能影响业务，因此所有异常都在这里吞掉并记录 warn。
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    private static final int MAX_LENGTH = 2000;

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Log logAnnotation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            saveLog(joinPoint, error == null ? result : error.getMessage(), System.currentTimeMillis() - start);
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, Object returnValue, long costTime) {
        try {
            OperateLog operateLog = new OperateLog();
            operateLog.setOperateTime(LocalDateTime.now());
            operateLog.setClassName(joinPoint.getTarget().getClass().getName());
            operateLog.setMethodName(joinPoint.getSignature().getName());
            operateLog.setMethodParams(toJson(joinPoint.getArgs()));
            operateLog.setReturnValue(truncate(toJson(returnValue)));
            operateLog.setCostTime(costTime);

            UserContext.CurrentUser currentUser = UserContext.get();
            if (currentUser != null) {
                operateLog.setOperateEmpId(currentUser.id());
            }

            operateLogMapper.insert(operateLog);
        } catch (Exception e) {
            log.warn("操作日志记录失败：{}", e.getMessage());
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return maskSensitive(truncate(objectMapper.writeValueAsString(value)));
        } catch (Exception e) {
            return maskSensitive(truncate(String.valueOf(value)));
        }
    }

    /**
     * 日志中不保留密码、身份证、手机号等敏感信息
     */
    private String maskSensitive(String text) {
        if (text == null) {
            return null;
        }
        return text
                .replaceAll("(?i)(\"password\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"")
                .replaceAll("(?i)(\"idCard\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"")
                .replaceAll("(?i)(\"phone\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
    }

    private String truncate(String text) {
        if (text == null || text.length() <= MAX_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_LENGTH);
    }
}
