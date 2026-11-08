package com.itheima.mapper;

import com.itheima.pojo.Student;
import com.itheima.pojo.StudentQueryParam;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface StudentMapper {

    //条件查询学员信息（带班级名称）
    List<Student> list(StudentQueryParam studentQueryParam);

    //根据id查询学员
    Student getById(Integer id);

    //新增学员
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into student (name, no, gender, phone, id_card, is_college, address, degree, graduation_date, clazz_id, create_time, update_time) " +
            "values (#{name}, #{no}, #{gender}, #{phone}, #{idCard}, #{isCollege}, #{address}, #{degree}, #{graduationDate}, #{clazzId}, #{createTime}, #{updateTime})")
    void insert(Student student);

    //修改学员基本信息
    void updateById(Student student);

    //违纪处理：违纪次数 +1，违纪扣分 +score
    //多个参数必须加 @Param，否则 XML 里取不到参数名
    void updateViolation(@Param("id") Integer id, @Param("score") Integer score, @Param("updateTime") LocalDateTime updateTime);

    //批量删除学员
    void deleteByIds(List<Integer> ids);

    //按学历统计学员人数
    List<Map<String, Object>> countStudentDegreeData();

    //统计指定班级的学员人数
    Integer countByClazzId(Integer clazzId);

    //统计全部学员人数
    @Select("select count(*) from student")
    Integer countAll();

    //查询违纪扣分最高的学员（AI 数据分析用）
    List<Map<String, Object>> topViolation(@Param("limit") int limit);

    List<Map<String, Object>> reportDegrees(@Param("clazzId") Integer clazzId);

    List<Map<String, Object>> reportViolations(@Param("clazzId") Integer clazzId, @Param("limit") int limit);

    Integer countForAi(@Param("clazzId") Integer clazzId, @Param("degree") Integer degree,
                       @Param("gender") Integer gender);

}
