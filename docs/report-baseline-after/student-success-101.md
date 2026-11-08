# student-success-101

Mode: live

SQL count: 4

## SQL

select * from clazz where id = ?

select count(*) from student where clazz_id = ?

select (case degree when 1 then '初中' when 2 then '高中' when 3 then '大专' when 4 then '本科' when 5 then '硕士' when 6 then '博士' else '其他' end) name, count(*) value from student where clazz_id = ? group by degree order by degree

select s.name name, s.violation_count violationCount, s.violation_score violationScore, c.name clazzName from student s left join clazz c on s.clazz_id = c.id where s.violation_count > 0 and s.clazz_id = ? order by s.violation_score desc, s.violation_count desc, s.id limit ?

## Response

# 固定模型响应

用于验证报告调用流程。

## Model input

你是智培云 · 智能培训管理平台的数据分析师。请根据系统数据快照，生成一份专业的学员学情分析报告。要求：
1. 使用 Markdown 输出，包含标题、总体概况、数据分析表格、问题与建议；
2. 所有数字必须来自快照，不得编造；
3. 使用简体中文，语气专业、简洁；
4. 不要输出 JSON 或代码块包裹整篇报告。

统计范围必须与以下基础报告一致，不得扩展到其他班级。

# 学员学情分析报告

> 生成时间：2026-11-08 15:40 · 数据来源：智培云 · 智能培训管理平台

## 一、总体概况

- 学员总数：4 人
- 班级数量：1 个
- 平均每班人数：4.0 人
- 聚焦班级：测试Java班（4 人）

## 二、班级人数分布

| 班级 | 人数 |
| --- | --- |
| 测试Java班 | 4 |

## 三、学历结构

| 学历 | 人数 | 占比 |
| --- | --- | --- |
| 大专 | 1 | 25.0% |
| 本科 | 2 | 50.0% |
| 硕士 | 1 | 25.0% |

## 四、违纪情况

- Java学员丙（测试Java班）：违纪 2 次，扣 15 分
- Java学员乙（测试Java班）：违纪 1 次，扣 5 分

## 五、分析建议

1. 「测试Java班」人数最多（4 人），建议重点关注班级容量与教学资源分配。
2. 学员学历以「本科」为主，可根据学员基础调整课程节奏。
3. 存在 2 名以上有违纪记录的学员，建议及时沟通并跟进。
