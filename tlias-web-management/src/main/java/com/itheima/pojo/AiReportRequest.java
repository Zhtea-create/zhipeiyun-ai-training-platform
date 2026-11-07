package com.itheima.pojo;

import lombok.Data;

@Data
public class AiReportRequest {

    private String type;        // student / employee
    private Integer clazzId;    // 学员报告可指定班级
    private String begin;       // 预留时间范围
    private String end;
}
