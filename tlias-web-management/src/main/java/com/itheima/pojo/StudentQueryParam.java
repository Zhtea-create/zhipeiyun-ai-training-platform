package com.itheima.pojo;

import lombok.Data;

@Data
public class StudentQueryParam {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String name; //姓名
    private Integer degree; //最高学历
    private Integer clazzId; //班级ID
}
