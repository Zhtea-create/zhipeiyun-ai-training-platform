package com.itheima.mapper;

import com.itheima.pojo.EmpExpr;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EmpExprMapper {

    //批量保存
    void insertBatch(List<EmpExpr> exprList);

    //根据员工id删除
    void deleteByEmpIds(List<Integer> empIds);
}
