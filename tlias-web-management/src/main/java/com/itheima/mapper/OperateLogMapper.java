package com.itheima.mapper;

import com.itheima.pojo.LogQueryParam;
import com.itheima.pojo.OperateLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OperateLogMapper {

    // 新增操作日志
    void insert(OperateLog operateLog);

    // 分页查询操作日志（关联 emp 表带出操作人姓名）
    List<OperateLog> page(LogQueryParam queryParam);

    // 查询最近 N 条操作日志（AI 数据上下文用）
    List<OperateLog> recent(@Param("limit") int limit);
}
