package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.exception.BusinessException;
import com.itheima.mapper.ClazzMapper;
import com.itheima.pojo.Clazz;
import com.itheima.pojo.ClazzQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.service.ClazzService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClazzServiceImpl implements ClazzService {

    @Autowired
    private ClazzMapper clazzMapper;

    @Override
    public PageResult<Clazz> page(ClazzQueryParam clazzQueryParam) {

        PageHelper.startPage(clazzQueryParam.getPage(), clazzQueryParam.getPageSize());

        List<Clazz> clazzList = clazzMapper.list(clazzQueryParam);

        Page<Clazz> clazzPage = (Page<Clazz>) clazzList;
        return new PageResult<Clazz>(clazzPage.getTotal(), clazzPage.getResult());
    }

    //新增班级
    @Override
    public void save(Clazz clazz) {
        clazz.setCreateTime(LocalDateTime.now());
        clazz.setUpdateTime(LocalDateTime.now());
        clazzMapper.insert(clazz);
    }

    //查询所有班级
    @Override
    public List<Clazz> listAll() {
        return clazzMapper.listAll();
    }

    //根据id查询班级
    @Override
    public Clazz getInfo(Integer id) {
        return clazzMapper.getById(id);
    }

    //修改班级
    @Override
    public void update(Clazz clazz) {
        clazz.setUpdateTime(LocalDateTime.now());
        clazzMapper.updateById(clazz);
    }

    //删除班级
    //班级下还有学员时不允许删除（两张表之间没有外键，数据库不会拦，只能在这里挡）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {

        Integer count = clazzMapper.countStudentByClazzId(id);
        if (count > 0) {
            throw new BusinessException("该班级下有学生，不能删除");
        }

        clazzMapper.deleteById(id);
    }

}
