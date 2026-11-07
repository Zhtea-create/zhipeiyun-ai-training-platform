package com.itheima.mapper;

import com.itheima.pojo.Clazz;
import com.itheima.pojo.ClazzQueryParam;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

import java.util.List;
import java.util.Map;

@Mapper
public interface ClazzMapper {

    public List<Clazz> list(ClazzQueryParam clazzQueryParam);

    //查询所有班级（学员页的"班级"下拉框用）
    //注意：方法名不能叫 list()，MyBatis 的语句 ID 是 namespace.方法名（不带参数），重载会和上面的 list(参数) 撞车
    List<Clazz> listAll();

    //根据id查询班级
    Clazz getById(Integer id);

    //新增班级
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into clazz (name, room, begin_date, end_date, master_id, subject, create_time, update_time) " +
            "values (#{name}, #{room}, #{beginDate}, #{endDate}, #{masterId}, #{subject}, #{createTime}, #{updateTime})")
    void insert(Clazz clazz);

    //修改班级
    void updateById(Clazz clazz);

    //删除班级
    void deleteById(Integer id);

    //统计该班级下的学员数量（删除班级前校验用）
    Integer countStudentByClazzId(Integer clazzId);

    //按班级统计学员人数
    List<Map<String, Object>> countStudentCountData();

    //统计全部班级数量
    Integer countAll();

}
