package com.itheima.service;

import com.itheima.pojo.LogQueryParam;
import com.itheima.pojo.OperateLog;
import com.itheima.pojo.PageResult;

import java.util.List;

public interface LogService {

    // 分页查询操作日志
    PageResult<OperateLog> page(LogQueryParam queryParam);

    // 保存操作日志
    void save(OperateLog operateLog);

    // 查询最近 N 条日志
    List<OperateLog> recent(int limit);
}
