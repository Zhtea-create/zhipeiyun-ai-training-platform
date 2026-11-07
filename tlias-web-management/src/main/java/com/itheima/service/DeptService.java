package com.itheima.service;

import com.itheima.pojo.Dept;

import java.util.List;

public interface DeptService {
    void add(Dept dept);

    List<Dept> findAll();

    void deleteById(Integer id);

    Dept getById(Integer id);

    void update(Dept dept);
}
