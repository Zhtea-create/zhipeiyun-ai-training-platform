package com.itheima.controller;


import com.itheima.annotation.Log;
import com.itheima.pojo.Clazz;
import com.itheima.pojo.ClazzQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.service.ClazzService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/clazzs")
public class ClazzController {

    @Autowired
    private ClazzService clazzService;

    //分页查询班级信息
    @GetMapping
    public Result page(ClazzQueryParam clazzQueryParam){

        log.info("分页查询班级信息，参数：{}", clazzQueryParam);
        PageResult<Clazz> pageResult = clazzService.page(clazzQueryParam);
        return Result.success(pageResult);

    }

    //查询所有班级（学员页的"班级"下拉框用）
    @GetMapping("/list")
    public Result listAll(){
        log.info("查询所有班级");
        List<Clazz> clazzList = clazzService.listAll();
        return Result.success(clazzList);
    }

    //新增班级
    @Log
    @PostMapping
    public Result save(@RequestBody Clazz clazz){
        log.info("新增班级，参数：{}", clazz);
        clazzService.save(clazz);
        return Result.success();
    }

    //根据id查询班级
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Integer id){
        log.info("根据id查询班级，参数：{}", id);
        Clazz clazz = clazzService.getInfo(id);
        return Result.success(clazz);
    }

    //修改班级
    @Log
    @PutMapping
    public Result update(@RequestBody Clazz clazz){
        log.info("修改班级，参数：{}", clazz);
        clazzService.update(clazz);
        return Result.success();
    }

    //删除班级
    @Log
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id){
        log.info("删除班级，参数：{}", id);
        clazzService.delete(id);
        return Result.success();
    }

}
