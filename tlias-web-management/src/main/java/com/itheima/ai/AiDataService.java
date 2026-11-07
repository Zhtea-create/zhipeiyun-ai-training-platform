package com.itheima.ai;

import com.itheima.mapper.ClazzMapper;
import com.itheima.mapper.DeptMapper;
import com.itheima.mapper.EmpMapper;
import com.itheima.mapper.OperateLogMapper;
import com.itheima.mapper.StudentMapper;
import com.itheima.pojo.Clazz;
import com.itheima.pojo.Dept;
import com.itheima.pojo.OperateLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 数据层：负责从数据库汇总安全的统计数据，并生成演示模式回答 / 报告。
 * 不把密码、身份证、手机号等敏感字段交给模型。
 */
@Slf4j
@Service
public class AiDataService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private EmpMapper empMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private ClazzMapper clazzMapper;

    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private OperateLogMapper operateLogMapper;

    // ==================== 数据快照 ====================

    public String buildSnapshot() {
        StringBuilder sb = new StringBuilder("## 系统数据快照\n\n");
        sb.append("- 员工总数：").append(empCount()).append(" 人\n");
        sb.append("- 学员总数：").append(studentCount()).append(" 人\n");
        sb.append("- 班级总数：").append(clazzCount()).append(" 个\n");
        sb.append("- 部门总数：").append(deptCount()).append(" 个\n\n");

        List<Map<String, Object>> jobs = jobStats();
        if (!jobs.isEmpty()) {
            sb.append("### 员工职位分布\n");
            jobs.forEach(item -> sb.append("- ")
                    .append(item.get("pos")).append("：").append(item.get("num")).append(" 人\n"));
            sb.append("\n");
        }

        List<Map<String, Object>> genders = genderStats();
        if (!genders.isEmpty()) {
            sb.append("### 员工性别分布\n");
            genders.forEach(item -> sb.append("- ")
                    .append(item.get("name")).append("：").append(item.get("value")).append(" 人\n"));
            sb.append("\n");
        }

        List<Map<String, Object>> classes = classStats();
        if (!classes.isEmpty()) {
            sb.append("### 班级人数分布\n");
            classes.forEach(item -> sb.append("- ")
                    .append(item.get("name")).append("：").append(item.get("value")).append(" 人\n"));
            sb.append("\n");
        }

        List<Map<String, Object>> degrees = degreeStats();
        if (!degrees.isEmpty()) {
            sb.append("### 学员学历分布\n");
            degrees.forEach(item -> sb.append("- ")
                    .append(item.get("name")).append("：").append(item.get("value")).append(" 人\n"));
            sb.append("\n");
        }

        List<Map<String, Object>> violations = violationStats(5);
        if (!violations.isEmpty()) {
            sb.append("### 违纪学员（按扣分排序）\n");
            violations.forEach(item -> sb.append("- ")
                    .append(item.get("name"))
                    .append("（").append(item.get("clazzName")).append("）")
                    .append("：违纪 ").append(item.get("violationCount")).append(" 次，扣 ")
                    .append(item.get("violationScore")).append(" 分\n"));
            sb.append("\n");
        }

        List<OperateLog> logs = recentLogs(5);
        if (!logs.isEmpty()) {
            sb.append("### 最近操作日志\n");
            logs.forEach(item -> sb.append("- ")
                    .append(item.getOperateTime() == null ? "" : item.getOperateTime().format(TIME_FORMAT))
                    .append(" ").append(item.getOperateEmpName() == null ? "未知" : item.getOperateEmpName())
                    .append(" ").append(simpleClass(item.getClassName()))
                    .append("#").append(item.getMethodName())
                    .append("，耗时 ").append(item.getCostTime()).append("ms\n"));
        }

        return sb.toString();
    }

    // ==================== 演示模式问答 ====================

    public String mockAnswer(String question) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            return "你可以问我：学员总数、班级人数排名、学员学历分布、员工职位与性别比例、最近操作日志等。";
        }

        List<String> parts = new ArrayList<>();

        if (containsAny(q, "学历")) {
            parts.add(degreeAnswer());
        }
        if (containsAny(q, "性别")) {
            parts.add(genderAnswer());
        }
        if (containsAny(q, "职位")) {
            parts.add(jobAnswer());
        }
        if (containsAny(q, "违纪")) {
            parts.add(violationAnswer());
        }
        if (containsAny(q, "日志", "操作记录", "操作日志")) {
            parts.add(logAnswer());
        }
        if (containsAny(q, "部门")) {
            parts.add(deptAnswer());
        }
        if (containsAny(q, "哪个班", "人数最多", "班级人数", "每班", "班级分布", "班的人数")) {
            parts.add(classRankAnswer());
        }

        if (parts.isEmpty()) {
            if (containsAny(q, "多少学员", "学员总数", "学员人数", "多少学生", "学生总数", "学生人数")) {
                parts.add("**学员总数：** " + studentCount() + " 人");
            }
            if (containsAny(q, "多少员工", "员工总数", "员工人数")) {
                parts.add("**员工总数：** " + empCount() + " 人");
            }
            if (containsAny(q, "多少班级", "班级总数", "班级数量")) {
                parts.add("**班级总数：** " + clazzCount() + " 个");
            }
        }

        if (parts.isEmpty()) {
            return overviewAnswer();
        }

        return String.join("\n\n", parts)
                + "\n\n> 当前为演示模式：以上数字来自数据库实时聚合，未调用大模型。";
    }

    private String overviewAnswer() {
        return "**系统概览**\n"
                + "- 员工：" + empCount() + " 人\n"
                + "- 学员：" + studentCount() + " 人\n"
                + "- 班级：" + clazzCount() + " 个\n"
                + "- 部门：" + deptCount() + " 个\n\n"
                + "你可以继续问：哪个班人数最多、学员学历分布、员工职位比例、最近操作日志……";
    }

    private String degreeAnswer() {
        List<Map<String, Object>> degrees = degreeStats();
        if (degrees.isEmpty()) {
            return "暂无学员学历数据。";
        }
        String lines = degrees.stream()
                .map(item -> "- " + item.get("name") + "：" + item.get("value") + " 人")
                .collect(Collectors.joining("\n"));
        return "**学员学历分布**\n" + lines;
    }

    private String genderAnswer() {
        List<Map<String, Object>> genders = genderStats();
        if (genders.isEmpty()) {
            return "暂无员工性别数据。";
        }
        String lines = genders.stream()
                .map(item -> "- " + item.get("name") + "：" + item.get("value") + " 人")
                .collect(Collectors.joining("\n"));
        return "**员工性别比例**\n" + lines;
    }

    private String jobAnswer() {
        List<Map<String, Object>> jobs = jobStats();
        if (jobs.isEmpty()) {
            return "暂无员工职位数据。";
        }
        String lines = jobs.stream()
                .map(item -> "- " + item.get("pos") + "：" + item.get("num") + " 人")
                .collect(Collectors.joining("\n"));
        return "**员工职位分布**\n" + lines;
    }

    private String classRankAnswer() {
        List<Map<String, Object>> classes = classStats();
        if (classes.isEmpty()) {
            return "暂无班级人数数据。";
        }
        String lines = classes.stream()
                .map(item -> "- " + item.get("name") + "：" + item.get("value") + " 人")
                .collect(Collectors.joining("\n"));
        Map<String, Object> top = classes.get(0);
        return "**班级人数排名**\n" + lines
                + "\n\n人数最多的是「" + top.get("name") + "」，共 " + top.get("value") + " 人。";
    }

    private String violationAnswer() {
        List<Map<String, Object>> violations = violationStats(5);
        if (violations.isEmpty()) {
            return "当前没有违纪记录，学员纪律情况良好。";
        }
        String lines = violations.stream()
                .map(item -> "- " + item.get("name") + "（" + item.get("clazzName") + "）：违纪 "
                        + item.get("violationCount") + " 次，扣 " + item.get("violationScore") + " 分")
                .collect(Collectors.joining("\n"));
        return "**违纪情况（按扣分排序）**\n" + lines;
    }

    private String logAnswer() {
        List<OperateLog> logs = recentLogs(5);
        if (logs.isEmpty()) {
            return "暂无操作日志。";
        }
        String lines = logs.stream()
                .map(item -> "- " + (item.getOperateTime() == null ? "" : item.getOperateTime().format(TIME_FORMAT))
                        + " " + (item.getOperateEmpName() == null ? "未知" : item.getOperateEmpName())
                        + " 执行 " + simpleClass(item.getClassName()) + "#" + item.getMethodName()
                        + "，耗时 " + item.getCostTime() + "ms")
                .collect(Collectors.joining("\n"));
        return "**最近操作记录**\n" + lines;
    }

    private String deptAnswer() {
        List<Dept> depts = depts();
        if (depts.isEmpty()) {
            return "暂无部门数据。";
        }
        String names = depts.stream().map(Dept::getName).collect(Collectors.joining("、"));
        return "**部门信息**\n当前共有 " + depts.size() + " 个部门：" + names + "。";
    }

    // ==================== 演示模式报告 ====================

    public String buildStudentReport(Integer clazzId) {
        int total = studentCount();
        int classCount = clazzCount();
        String average = classCount == 0 ? "0" : String.format("%.1f", total / (double) classCount);
        List<Map<String, Object>> classes = classStats();
        List<Map<String, Object>> degrees = degreeStats();
        List<Map<String, Object>> violations = violationStats(5);

        StringBuilder sb = new StringBuilder();
        sb.append("# 学员学情分析报告\n\n");
        sb.append("> 生成时间：").append(LocalDateTime.now().format(TIME_FORMAT))
                .append(" · 数据来源：智培云 · 智能培训管理平台\n\n");

        sb.append("## 一、总体概况\n\n");
        sb.append("- 学员总数：").append(total).append(" 人\n");
        sb.append("- 班级数量：").append(classCount).append(" 个\n");
        sb.append("- 平均每班人数：").append(average).append(" 人\n");

        if (clazzId != null) {
            Clazz clazz = safeGetClazz(clazzId);
            if (clazz != null) {
                sb.append("- 聚焦班级：").append(clazz.getName())
                        .append("（").append(safeCountByClazz(clazzId)).append(" 人）\n");
            }
        }

        sb.append("\n## 二、班级人数分布\n\n");
        if (classes.isEmpty()) {
            sb.append("暂无班级数据。\n");
        } else {
            sb.append("| 班级 | 人数 |\n| --- | --- |\n");
            classes.forEach(item -> sb.append("| ").append(item.get("name"))
                    .append(" | ").append(item.get("value")).append(" |\n"));
        }

        sb.append("\n## 三、学历结构\n\n");
        if (degrees.isEmpty()) {
            sb.append("暂无学历数据。\n");
        } else {
            sb.append("| 学历 | 人数 | 占比 |\n| --- | --- | --- |\n");
            degrees.forEach(item -> {
                int value = toInt(item.get("value"));
                double ratio = total == 0 ? 0 : value * 100.0 / total;
                sb.append("| ").append(item.get("name"))
                        .append(" | ").append(value)
                        .append(" | ").append(String.format("%.1f%%", ratio)).append(" |\n");
            });
        }

        sb.append("\n## 四、违纪情况\n\n");
        if (violations.isEmpty()) {
            sb.append("当前没有违纪记录，学员纪律情况良好。\n");
        } else {
            violations.forEach(item -> sb.append("- ").append(item.get("name"))
                    .append("（").append(item.get("clazzName")).append("）：违纪 ")
                    .append(item.get("violationCount")).append(" 次，扣 ")
                    .append(item.get("violationScore")).append(" 分\n"));
        }

        sb.append("\n## 五、分析建议\n\n");
        if (!classes.isEmpty()) {
            Map<String, Object> top = classes.get(0);
            sb.append("1. 「").append(top.get("name")).append("」人数最多（")
                    .append(top.get("value")).append(" 人），建议重点关注班级容量与教学资源分配。\n");
        }
        if (!degrees.isEmpty()) {
            Map<String, Object> topDegree = degrees.stream()
                    .max((a, b) -> Integer.compare(toInt(a.get("value")), toInt(b.get("value"))))
                    .orElse(degrees.get(0));
            sb.append("2. 学员学历以「").append(topDegree.get("name")).append("」为主，可根据学员基础调整课程节奏。\n");
        }
        if (!violations.isEmpty()) {
            sb.append("3. 存在 ").append(violations.size()).append(" 名以上有违纪记录的学员，建议及时沟通并跟进。\n");
        } else {
            sb.append("3. 当前违纪记录较少，整体纪律良好，可继续保持。\n");
        }

        return sb.toString();
    }

    public String buildEmployeeReport() {
        List<Map<String, Object>> jobs = jobStats();
        List<Map<String, Object>> genders = genderStats();
        int total = empCount();

        StringBuilder sb = new StringBuilder();
        sb.append("# 员工团队分析报告\n\n");
        sb.append("> 生成时间：").append(LocalDateTime.now().format(TIME_FORMAT))
                .append(" · 数据来源：智培云 · 智能培训管理平台\n\n");

        sb.append("## 一、总体概况\n\n");
        sb.append("- 员工总数：").append(total).append(" 人\n");
        sb.append("- 部门数量：").append(deptCount()).append(" 个\n\n");

        sb.append("## 二、职位分布\n\n");
        if (jobs.isEmpty()) {
            sb.append("暂无职位数据。\n");
        } else {
            sb.append("| 职位 | 人数 |\n| --- | --- |\n");
            jobs.forEach(item -> sb.append("| ").append(item.get("pos"))
                    .append(" | ").append(item.get("num")).append(" |\n"));
        }

        sb.append("\n## 三、性别结构\n\n");
        if (genders.isEmpty()) {
            sb.append("暂无性别数据。\n");
        } else {
            sb.append("| 性别 | 人数 |\n| --- | --- |\n");
            genders.forEach(item -> sb.append("| ").append(item.get("name"))
                    .append(" | ").append(item.get("value")).append(" |\n"));
        }

        sb.append("\n## 四、分析建议\n\n");
        if (!jobs.isEmpty()) {
            Map<String, Object> topJob = jobs.stream()
                    .max((a, b) -> Integer.compare(toInt(a.get("num")), toInt(b.get("num"))))
                    .orElse(jobs.get(0));
            sb.append("1. 「").append(topJob.get("pos")).append("」人数最多（")
                    .append(topJob.get("num")).append(" 人），是团队的核心岗位，建议关注人才梯队建设。\n");
        }
        sb.append("2. 建议结合部门与职位结构，定期评估各岗位人员配置是否均衡。\n");
        sb.append("3. 可关注员工入职时间与流动情况，提前做好招聘与培训计划。\n");

        return sb.toString();
    }

    public String buildSystemPrompt() {
        return "你是智培云 · 智能培训管理平台的数据分析助手。请只根据下面提供的系统数据快照回答用户问题，"
                + "不要编造数字；如果数据中没有答案，请明确说明。回答使用简体中文，条理清晰，适当使用 Markdown。\n\n"
                + buildSnapshot();
    }

    public String buildReportPrompt(String type) {
        String typeName = "student".equalsIgnoreCase(type) ? "学员学情" : "员工团队";
        return "你是智培云 · 智能培训管理平台的数据分析师。请根据系统数据快照，生成一份专业的"
                + typeName + "分析报告。要求：\n"
                + "1. 使用 Markdown 输出，包含标题、总体概况、数据分析表格、问题与建议；\n"
                + "2. 所有数字必须来自快照，不得编造；\n"
                + "3. 使用简体中文，语气专业、简洁；\n"
                + "4. 不要输出 JSON 或代码块包裹整篇报告。\n\n"
                + buildSnapshot();
    }

    // ==================== 数据查询 ====================

    private int empCount() {
        try {
            Integer value = empMapper.countAll();
            return value == null ? 0 : value;
        } catch (Exception e) {
            log.warn("查询员工总数失败：{}", e.getMessage());
            return 0;
        }
    }

    private int studentCount() {
        try {
            Integer value = studentMapper.countAll();
            return value == null ? 0 : value;
        } catch (Exception e) {
            log.warn("查询学员总数失败：{}", e.getMessage());
            return 0;
        }
    }

    private int clazzCount() {
        try {
            Integer value = clazzMapper.countAll();
            return value == null ? 0 : value;
        } catch (Exception e) {
            log.warn("查询班级总数失败：{}", e.getMessage());
            return 0;
        }
    }

    private int deptCount() {
        try {
            return deptMapper.findAll().size();
        } catch (Exception e) {
            log.warn("查询部门失败：{}", e.getMessage());
            return 0;
        }
    }

    private List<Dept> depts() {
        try {
            return deptMapper.findAll();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Map<String, Object>> jobStats() {
        try {
            return empMapper.countEmpJobData();
        } catch (Exception e) {
            log.warn("查询员工职位统计失败：{}", e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> genderStats() {
        try {
            return empMapper.countEmpGenderData();
        } catch (Exception e) {
            log.warn("查询员工性别统计失败：{}", e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> degreeStats() {
        try {
            return studentMapper.countStudentDegreeData();
        } catch (Exception e) {
            log.warn("查询学员学历统计失败：{}", e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> classStats() {
        try {
            return clazzMapper.countStudentCountData();
        } catch (Exception e) {
            log.warn("查询班级人数统计失败：{}", e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> violationStats(int limit) {
        try {
            return studentMapper.topViolation(limit);
        } catch (Exception e) {
            log.warn("查询违纪统计失败：{}", e.getMessage());
            return List.of();
        }
    }

    private List<OperateLog> recentLogs(int limit) {
        try {
            return operateLogMapper.recent(limit);
        } catch (Exception e) {
            log.warn("查询操作日志失败：{}", e.getMessage());
            return List.of();
        }
    }

    private Clazz safeGetClazz(Integer clazzId) {
        try {
            return clazzMapper.getById(clazzId);
        } catch (Exception e) {
            return null;
        }
    }

    private int safeCountByClazz(Integer clazzId) {
        try {
            Integer value = studentMapper.countByClazzId(clazzId);
            return value == null ? 0 : value;
        } catch (Exception e) {
            return 0;
        }
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String simpleClass(String className) {
        if (className == null) {
            return "-";
        }
        int index = className.lastIndexOf('.');
        return index >= 0 ? className.substring(index + 1) : className;
    }
}
