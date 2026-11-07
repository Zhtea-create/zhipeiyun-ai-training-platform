package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Emp {
    private Integer id;              // ID，主键
    private String username;         // 用户名
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;         // 密码（只接收，不回传给前端）
    private String name;             // 姓名
    private Short gender;            // 性别，1:男，2:女
    private String phone;            // 手机号
    private Short job;               // 职位，1 班主任，2 讲师，3 学工主管，4 教研主管，5 咨询师
    private Integer salary;          // 薪资
    private String image;            // 头像
    private LocalDate entryDate;     // 入职日期
    private Integer deptId;          // 部门ID
    private LocalDateTime createTime;// 创建时间
    private LocalDateTime updateTime;// 修改时间

    //部门名称
    private String deptName;
    //封装工作经历信息
    private List<EmpExpr> exprList;

}
