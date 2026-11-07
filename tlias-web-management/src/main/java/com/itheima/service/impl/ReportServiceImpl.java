package com.itheima.service.impl;

import com.itheima.mapper.ClazzMapper;
import com.itheima.mapper.EmpMapper;
import com.itheima.mapper.StudentMapper;
import com.itheima.pojo.JobOption;
import com.itheima.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private EmpMapper empMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private ClazzMapper clazzMapper;

    @Override
    public JobOption getEmpJobData() {

        List<Map<String, Object>> list = empMapper.countEmpJobData();

        List<Object> jobList = list.stream().map(map -> map.get("pos")).toList();
        List<Object> dataList = list.stream().map(map -> map.get("num")).toList();

        return new JobOption(jobList, dataList);

    }

    @Override
    public List<Map<String, Object>> getEmpGenderData() {
        return empMapper.countEmpGenderData();
    }

    @Override
    public List<Map<String, Object>> getStudentDegreeData() {
        return studentMapper.countStudentDegreeData();
    }

    @Override
    public Map<String, Object> getStudentCountData() {
        List<Map<String, Object>> list = clazzMapper.countStudentCountData();

        List<Object> clazzList = list.stream().map(map -> map.get("name")).toList();
        List<Object> dataList = list.stream().map(map -> map.get("value")).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("clazzList", clazzList);
        result.put("dataList", dataList);
        return result;
    }

}
