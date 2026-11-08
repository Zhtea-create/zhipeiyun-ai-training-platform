-- Only for the isolated H2 report tests. Never execute against the business database.
create table dept (id int primary key, name varchar(50), create_time timestamp, update_time timestamp);
create table emp (id int primary key, name varchar(50), job int, gender int);
create table clazz (id int primary key, name varchar(50));
create table student (id int primary key, name varchar(50), clazz_id int, degree int,
                      violation_count int, violation_score int);
create table operate_log (id int primary key, operate_emp_id int, operate_time timestamp,
 class_name varchar(100), method_name varchar(100), method_params varchar(100),
 return_value varchar(100), cost_time bigint);

insert into dept values (1, '教学部', '2026-01-01 00:00:00', '2026-01-01 00:00:00'),
                        (2, '教研部', '2026-01-01 00:00:00', '2026-01-01 00:00:00');
insert into emp values (1, '测试员工甲', 1, 1), (2, '测试员工乙', 2, 2),
                       (3, '测试员工丙', 2, 1), (4, '测试员工丁', 4, 2);
insert into clazz values (101, '测试Java班'), (102, '测试Python班'), (103, '测试空班');
-- Java: 4 students; bachelor 2, college 1, master 1; two violations.
-- Python: 2 students; high school 1, college 1; one higher-scoring violation.
insert into student values
 (1, 'Java学员甲', 101, 4, 0, 0),
 (2, 'Java学员乙', 101, 4, 1, 5),
 (3, 'Java学员丙', 101, 3, 2, 15),
 (4, 'Java学员丁', 101, 5, 0, 0),
 (5, 'Python学员甲', 102, 2, 3, 30),
 (6, 'Python学员乙', 102, 3, 0, 0);
insert into operate_log values (1, 1, '2026-01-01 12:00:00',
 'com.itheima.controller.StudentController', 'insert', '{}', '{}', 5);
