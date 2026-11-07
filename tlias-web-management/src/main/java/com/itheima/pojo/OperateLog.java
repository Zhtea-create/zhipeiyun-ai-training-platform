package com.itheima.pojo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
public class OperateLog {

    private Integer id;              // 主键ID
    private Integer operateEmpId;    // 操作人ID
    private LocalDateTime operateTime; // 操作时间
    private String className;        // 类名
    private String methodName;       // 方法名
    private String methodParams;     // 方法请求参数
    private String returnValue;      // 返回值
    private Long costTime;           // 执行耗时(ms)

    private String operateEmpName;   // 操作人姓名（关联 emp 表查询）
}
