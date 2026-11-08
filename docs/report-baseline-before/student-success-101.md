# student-success-101

Mode: live

SQL count: 17

## SQL

select count(*) from student

select count(*) from clazz

select c.name name, count(s.id) value from clazz c left join student s on c.id = s.clazz_id group by c.id, c.name order by count(s.id) desc

select (case degree when 1 then '初中' when 2 then '高中' when 3 then '大专' when 4 then '本科' when 5 then '硕士' when 6 then '博士' else '其他' end) name, count(*) value from student group by degree order by degree

select s.name name, s.violation_count violationCount, s.violation_score violationScore, c.name clazzName from student s left join clazz c on s.clazz_id = c.id where s.violation_count > 0 order by s.violation_score desc, s.violation_count desc limit ?

select * from clazz where id = ?

select count(*) from student where clazz_id = ?

select count(*) from emp

select count(*) from student

select count(*) from clazz

select id, name, create_time, update_time from dept order by update_time desc

select (case when job=1 then '班主任' when job=2 then '讲师' when job=3 then '学工主管' when job=4 then '教研主管' when job=5 then '咨询师' else '其他' end) pos, count(*) num from emp group by job order by num

select (case when gender=1 then '男性员工' when gender=2 then '女性员工' else '其他' end) name, count(*) value from emp group by gender

select c.name name, count(s.id) value from clazz c left join student s on c.id = s.clazz_id group by c.id, c.name order by count(s.id) desc

select (case degree when 1 then '初中' when 2 then '高中' when 3 then '大专' when 4 then '本科' when 5 then '硕士' when 6 then '博士' else '其他' end) name, count(*) value from student group by degree order by degree

select s.name name, s.violation_count violationCount, s.violation_score violationScore, c.name clazzName from student s left join clazz c on s.clazz_id = c.id where s.violation_count > 0 order by s.violation_score desc, s.violation_count desc limit ?

select ol.*, e.name operate_emp_name from operate_log ol left join emp e on ol.operate_emp_id = e.id order by ol.operate_time desc limit ?

## Response

# 固定模型响应

用于验证报告调用流程。

## Model input

你是智培云 · 智能培训管理平台的数据分析师。请根据系统数据快照，生成一份专业的学员学情分析报告。要求：
1. 使用 Markdown 输出，包含标题、总体概况、数据分析表格、问题与建议；
2. 所有数字必须来自快照，不得编造；
3. 使用简体中文，语气专业、简洁；
4. 不要输出 JSON 或代码块包裹整篇报告。

## 系统数据快照

- 员工总数：4 人
- 学员总数：6 人
- 班级总数：3 个
- 部门总数：2 个

### 员工职位分布
- 班主任：1 人
- 教研主管：1 人
- 讲师：2 人

### 员工性别分布
- 男性员工：2 人
- 女性员工：2 人

### 班级人数分布
- 测试Java班：4 人
- 测试Python班：2 人
- 测试空班：0 人

### 学员学历分布
- 高中：1 人
- 大专：2 人
- 本科：2 人
- 硕士：1 人

### 违纪学员（按扣分排序）
- Python学员甲（测试Python班）：违纪 3 次，扣 30 分
- Java学员丙（测试Java班）：违纪 2 次，扣 15 分
- Java学员乙（测试Java班）：违纪 1 次，扣 5 分

### 最近操作日志
- 2026-01-01 12:00 测试员工甲 StudentController#insert，耗时 5ms
