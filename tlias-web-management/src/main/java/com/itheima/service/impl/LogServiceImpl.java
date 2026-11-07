package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.OperateLogMapper;
import com.itheima.pojo.LogQueryParam;
import com.itheima.pojo.OperateLog;
import com.itheima.pojo.PageResult;
import com.itheima.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogServiceImpl implements LogService {

    @Autowired
    private OperateLogMapper operateLogMapper;

    @Override
    public PageResult<OperateLog> page(LogQueryParam queryParam) {
        PageHelper.startPage(queryParam.getPage(), queryParam.getPageSize());
        List<OperateLog> list = operateLogMapper.page(queryParam);
        Page<OperateLog> page = (Page<OperateLog>) list;
        return new PageResult<>(page.getTotal(), page.getResult());
    }

    @Override
    public void save(OperateLog operateLog) {
        operateLogMapper.insert(operateLog);
    }

    @Override
    public List<OperateLog> recent(int limit) {
        return operateLogMapper.recent(limit);
    }
}
