package com.itheima.ai;

import com.itheima.mapper.*;
import com.itheima.exception.BusinessException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 只读白名单工具；不允许模型提供 SQL 或返回完整员工/学员实体。 */
@Component
public class AiQueryTools {
    private final ClazzMapper clazzMapper;
    private final StudentMapper studentMapper;
    private final EmpMapper empMapper;
    private final DeptMapper deptMapper;
    private final OperateLogMapper logMapper;

    public AiQueryTools(ClazzMapper clazzMapper, StudentMapper studentMapper, EmpMapper empMapper,
                        DeptMapper deptMapper, OperateLogMapper logMapper) {
        this.clazzMapper = clazzMapper;
        this.studentMapper = studentMapper;
        this.empMapper = empMapper;
        this.deptMapper = deptMapper;
        this.logMapper = logMapper;
    }

    @Tool(description = "按班级名称查找候选 ID 和名称。空字符串列出前20个；多个候选应请用户确认，不能猜测。")
    public Map<String, Object> findClasses(@ToolParam(description = "班级名称或名称的一部分，列出班级时传空字符串") String keyword) {
        if (keyword == null || keyword.length() > 100) throw new BusinessException("班级名称长度不合法");
        var rows = clazzMapper.findForAi(keyword.trim());
        return Map.of("classes", rows.stream().limit(20).toList(), "truncated", rows.size() > 20);
    }

    @Tool(description = "按班级、学历、性别组合统计学员人数。不限制的条件省略或传null；返回0表示确实无人符合。")
    public Map<String, Object> countStudents(
            @ToolParam(required = false, description = "通过findClasses确认的班级ID；全校统计省略") Integer clazzId,
            @ToolParam(required = false, description = "学历：1初中、2高中、3大专、4本科、5硕士、6博士；不限则省略") Integer degree,
            @ToolParam(required = false, description = "性别：1男、2女；不限则省略") Integer gender) {
        checkClass(clazzId);
        if (degree != null && (degree < 1 || degree > 6)) throw new BusinessException("学历编码必须为1到6");
        if (gender != null && gender != 1 && gender != 2) throw new BusinessException("性别编码必须为1或2");
        return Map.of("count", studentMapper.countForAi(clazzId, degree, gender),
                "scope", clazzId == null ? "全校" : "班级ID=" + clazzId);
    }

    @Tool(description = "查询全校或指定班级的学员学历分布。")
    public List<Map<String, Object>> studentDegrees(
            @ToolParam(required = false, description = "已确认的班级ID；全校统计省略") Integer clazzId) {
        checkClass(clazzId);
        return studentMapper.reportDegrees(clazzId);
    }

    @Tool(description = "查询各班人数及排名，包括没有学员的班级。")
    public List<Map<String, Object>> classStudentCounts() {
        return clazzMapper.countStudentCountData();
    }

    @Tool(description = "查询全校或指定班级违纪扣分最高的前5名学员，按班级过滤后再取排名。")
    public List<Map<String, Object>> studentViolations(
            @ToolParam(required = false, description = "已确认的班级ID；全校查询省略") Integer clazzId) {
        checkClass(clazzId);
        return studentMapper.reportViolations(clazzId, 5);
    }

    @Tool(description = "查询员工总数、职位分布与性别分布，仅返回聚合数据。")
    public Map<String, Object> employeeStatistics() {
        return Map.of("total", empMapper.countAll(), "jobs", empMapper.countEmpJobData(),
                "genders", empMapper.countEmpGenderData());
    }

    @Tool(description = "查询部门ID与名称列表。")
    public List<Map<String, Object>> departments() {
        return deptMapper.findAll().stream()
                .map(dept -> Map.<String, Object>of("id", dept.getId(), "name", dept.getName())).toList();
    }

    @Tool(description = "查询最近5条操作的时间、方法和耗时，不返回请求参数或返回内容。")
    public List<Map<String, Object>> recentOperations() {
        return logMapper.recent(5).stream().map(log -> Map.<String, Object>of(
                "time", String.valueOf(log.getOperateTime()),
                "method", String.valueOf(log.getMethodName()),
                "costMs", log.getCostTime() == null ? 0L : log.getCostTime())).toList();
    }

    private void checkClass(Integer clazzId) {
        if (clazzId != null && (clazzId <= 0 || clazzMapper.getById(clazzId) == null)) {
            throw new BusinessException("班级不存在，请重新确认班级");
        }
    }
}
