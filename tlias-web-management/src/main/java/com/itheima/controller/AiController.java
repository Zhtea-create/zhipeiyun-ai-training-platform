package com.itheima.controller;

import com.itheima.annotation.Log;
import com.itheima.pojo.AiChatRequest;
import com.itheima.pojo.AiChatResponse;
import com.itheima.pojo.AiReportRequest;
import com.itheima.pojo.AiReportResponse;
import com.itheima.pojo.AiStatus;
import com.itheima.pojo.Result;
import com.itheima.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @GetMapping("/status")
    public Result status() {
        AiStatus status = aiService.status();
        return Result.success(status);
    }

    @Log
    @PostMapping("/chat")
    public Result chat(@RequestBody AiChatRequest request) {
        log.info("AI 对话，问题：{}", request.getMessage());
        AiChatResponse response = aiService.chat(request);
        return Result.success(response);
    }

    @Log
    @PostMapping("/report")
    public Result report(@RequestBody AiReportRequest request) {
        log.info("AI 生成报告，参数：{}", request);
        AiReportResponse response = aiService.report(request);
        return Result.success(response);
    }
}
