package com.itheima.controller;

import com.itheima.annotation.Log;
import com.itheima.pojo.Dept;
import com.itheima.pojo.Result;
import com.itheima.service.DeptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
public class DeptController {

    @Autowired
    private DeptService deptService;

//    @RequestMapping(value = "/depts", method = RequestMethod.GET)
    @GetMapping("/depts")
    public Result list(){
//        System.out.println("查询全部部门数据");
        log.info("查询全部部门数据");
        List<Dept> deptList = deptService.findAll();
        return Result.success(deptList);
    }

    @Log
    @DeleteMapping("/depts")
    public Result delete(Integer id){ //省略@RequestParam，因为前端传递参数名跟服务端方法形参名一致
//        System.out.println("id = " + id);
        log.info("删除了id = {}" , id);
        deptService.deleteById(id);
        return Result.success();
    }

    @Log
    @PostMapping("/depts")
    public Result add(@RequestBody Dept dept){
//        System.out.println("dept = " + dept);
        log.info("添加了dept = {}", dept);
        deptService.add(dept);
        return Result.success();
    }

    @GetMapping("/depts/{id}")
    public Result get(@PathVariable("id") Integer id){
//        System.out.println("Request id = " + id);
        log.info("Request id = {}" , id);
        Dept dept = deptService.getById(id);
        return Result.success(dept);
    }

    @Log
    @PutMapping("/depts")
    public Result update(@RequestBody Dept dept) {
//        System.out.println("dept = " + dept);
        log.info("更新了dept = {}", dept);
        deptService.update(dept);
        return Result.success();
    }



}
