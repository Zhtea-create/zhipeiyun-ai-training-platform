package com.itheima.pojo;

import lombok.Data;

/**
 * 操作日志分页查询参数
 */
@Data
public class LogQueryParam {

    private Integer page = 1;      // 页码
    private Integer pageSize = 10; // 每页记录数
}
