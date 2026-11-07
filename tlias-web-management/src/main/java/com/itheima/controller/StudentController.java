package com.itheima.controller;

import com.itheima.annotation.Log;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.pojo.Student;
import com.itheima.pojo.StudentQueryParam;
import com.itheima.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//学员管理Controller
@Slf4j
@RestController
@RequestMapping("/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    //分页查询学员信息
    @GetMapping
    public Result page(StudentQueryParam studentQueryParam){

        log.info("分页查询学员信息，参数：{}", studentQueryParam);
        PageResult<Student> pageResult = studentService.page(studentQueryParam);
        return Result.success(pageResult);

    }

    //新增学员
    @Log
    @PostMapping
    public Result save(@RequestBody Student student){
        log.info("新增学员，参数：{}", student);
        studentService.save(student);
        return Result.success();
    }

    //批量删除学员
    //接口文档是 /students/{ids}，id 走路径变量、逗号分隔（如 /students/3,4,6）
    //跟 EmpController 的 @RequestParam List<Integer> ids 写法不一样，别照抄那边
    @Log
    @DeleteMapping("/{ids}")
    public Result delete(@PathVariable List<Integer> ids){
        log.info("批量删除学员，参数：{}", ids);
        studentService.delete(ids);
        return Result.success();
    }

    //违纪处理
    @Log
    @PutMapping("/violation/{id}/{score}")
    public Result violation(@PathVariable Integer id, @PathVariable Integer score){
        log.info("违纪处理，id：{}，扣分：{}", id, score);
        studentService.violation(id, score);
        return Result.success();
    }

    //根据id查询学员
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Integer id){
        log.info("根据id查询学员，参数：{}", id);
        Student student = studentService.getInfo(id);
        return Result.success(student);
    }

    //修改学员
    @Log
    @PutMapping
    public Result update(@RequestBody Student student){
        log.info("修改学员，参数：{}", student);
        studentService.update(student);
        return Result.success();
    }

}
