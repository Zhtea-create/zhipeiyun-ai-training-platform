package com.itheima.controller;

import com.itheima.annotation.Log;
import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.service.EmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

//员工管理Controller
@Slf4j
@RestController
@RequestMapping("/emps")
public class EmpController {

    @Autowired
    private EmpService empService;

    @GetMapping
    public Result page(EmpQueryParam empQueryParam){

        log.info("分页查询员工数据，参数：{}", empQueryParam);
        PageResult<Emp> pageResult = empService.page(empQueryParam);
        return Result.success(pageResult);

    }

    //新增员工
    @Log
    @PostMapping
    public Result save(@RequestBody Emp emp){
        log.info("新增员工，参数：{}", emp);
        empService.save(emp);
        return Result.success();
    }

    //删除员工
    @Log
    @DeleteMapping
    public Result delete(@RequestParam List<Integer> ids){
        log.info("删除员工，参数：{}", ids);
        empService.delete(ids);
        return Result.success();
    }

    //查询所有员工（班级页的"班主任"下拉框用）
    @GetMapping("/list")
    public Result listAll(){
        log.info("查询所有员工");
        List<Emp> empList = empService.listAll();
        return Result.success(empList);
    }

    //根据id查询员工数据
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Integer id){
        log.info("根据id查询员工数据，参数：{}", id);
        Emp emp = empService.getInfo(id);

        return Result.success(emp);
    }

    //修改员工
    @Log
    @PutMapping
    public Result update(@RequestBody Emp emp){
        log.info("修改员工，参数：{}", emp);
        empService.update(emp);
        return Result.success();
    }




















}
