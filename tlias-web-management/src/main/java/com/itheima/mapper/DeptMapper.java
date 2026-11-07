package com.itheima.mapper;

import com.itheima.pojo.Dept;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DeptMapper {

    // 查询全部部门
    @Select("select id, name, create_time, update_time from dept order by update_time desc")
    List<Dept> findAll();

    // 根据id删除部门
    @Delete("delete from dept where id = #{id}")
    void deleteById(Integer id);

    // 添加部门
    @Insert("insert into dept (name, create_time, update_time) values (#{name}, #{createTime}, #{updateTime})")
    void insert(Dept dept);

    @Select("select id, name, create_time, update_time from dept where id = #{id}")
    Dept getById(Integer id);

    @Update("update dept set name = #{name}, update_time = #{updateTime} where id = #{id}")
    void update(Dept dept);
}
