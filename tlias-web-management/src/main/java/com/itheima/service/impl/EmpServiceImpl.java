package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.EmpExprMapper;
import com.itheima.mapper.EmpMapper;
import com.itheima.pojo.*;
import com.itheima.service.EmpService;
import com.itheima.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmpServiceImpl implements EmpService{

    @Autowired
    private EmpMapper empMapper;

    @Autowired
    private EmpExprMapper empExprMapper;

    //修改员工
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Emp emp) {
        //根据员工id修改基本信息
        emp.setUpdateTime(LocalDateTime.now());
        empMapper.updateById(emp);

        //根据员工id修改工作经历信息
        empExprMapper.deleteByEmpIds(Arrays.asList(emp.getId()));

        List<EmpExpr> exprList = emp.getExprList();
        if(!CollectionUtils.isEmpty(exprList)){
            //遍历集合，为empId赋值
            exprList.forEach(empExpr -> {
                empExpr.setEmpId(emp.getId());
            });
            empExprMapper.insertBatch(exprList);
        }

    }

    @Override
    public Emp getInfo(Integer id) {
        return empMapper.getById(id);
    }

    //查询所有员工
    @Override
    public List<Emp> listAll() {
        return empMapper.listAll();
    }

    //删除员工
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<Integer> ids) {
        //删除基本信息
        empMapper.deleteByIds(ids);

        //删除工作经历信息
        empExprMapper.deleteByEmpIds(ids);
    }

    //分页查询
    @Override
    public PageResult<Emp> page(EmpQueryParam empQueryParam){

        PageHelper.startPage(empQueryParam.getPage(), empQueryParam.getPageSize());

        List<Emp> empList = empMapper.list(empQueryParam);

        Page<Emp> p = (Page<Emp>) empList;
        return new PageResult<Emp>(p.getTotal(), p.getResult());

    }

    //登录
    @Override
    public LoginInfo login(Emp emp) {

        Emp e = empMapper.selectByUsernameAndPassword(emp);

        if(e != null){
            log.info("登录成功，用户名：{}", emp.getUsername());

            Map<String, Object> claims = new HashMap<>();
            claims.put("id", e.getId());
            claims.put("username", e.getUsername());
            claims.put("name", e.getName());
            String token = JwtUtils.generateToken(claims);

            return new LoginInfo(e.getId(), e.getUsername(), e.getName(), token);
        }

        return null;

    }



    //新增员工
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void save(Emp emp) {

        //基本信息
        emp.setCreateTime(LocalDateTime.now());
        emp.setUpdateTime(LocalDateTime.now());
        empMapper.insert(emp);

        //工作经历信息
        List<EmpExpr> exprList = emp.getExprList();
        if(!CollectionUtils.isEmpty(exprList)){
            //遍历集合，为empId赋值
            exprList.forEach(empExpr -> {
                empExpr.setEmpId(emp.getId());
            });
            empExprMapper.insertBatch(exprList);
        }

    }
}
