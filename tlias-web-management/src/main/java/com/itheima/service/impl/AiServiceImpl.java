package com.itheima.service.impl;

import com.itheima.ai.AiClient;
import com.itheima.ai.AiDataService;
import com.itheima.ai.AiMessage;
import com.itheima.ai.AiProperties;
import com.itheima.ai.ReportData;
import java.util.Locale;
import com.itheima.pojo.AiChatRequest;
import com.itheima.pojo.AiChatResponse;
import com.itheima.pojo.AiReportRequest;
import com.itheima.pojo.AiReportResponse;
import com.itheima.pojo.AiStatus;
import com.itheima.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private AiClient aiClient;

    @Autowired
    private AiDataService aiDataService;

    @Override
    public AiStatus status() {
        return new AiStatus(mode(), aiClient.isLive() ? aiProperties.getModel() : "mock");
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String question = request == null ? null : request.getMessage();
        if (question == null || question.isBlank()) {
            return new AiChatResponse("请输入你想了解的问题。", mode());
        }

        if (!aiClient.isLive()) {
            return new AiChatResponse(aiDataService.mockAnswer(question), "mock");
        }

        try {
            List<AiMessage> messages = new ArrayList<>();
            messages.add(new AiMessage("system", aiDataService.buildSystemPrompt()));

            List<AiMessage> history = request.getHistory();
            if (history != null && !history.isEmpty()) {
                int from = Math.max(0, history.size() - 8);
                for (AiMessage message : history.subList(from, history.size())) {
                    if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                        continue;
                    }
                    if ("user".equals(message.getRole()) || "assistant".equals(message.getRole())) {
                        messages.add(message);
                    }
                }
            }

            messages.add(new AiMessage("user", question));
            String answer = aiClient.chatWithTools(messages);
            return new AiChatResponse(answer, "live");
        } catch (Exception e) {
            log.warn("AI 对话调用失败，回退演示模式：{}", e.getMessage());
            return new AiChatResponse(
                    aiDataService.mockAnswer(question) + "\n\n> 真实模型暂时不可用，已回退到演示模式。",
                    "mock");
        }
    }

    @Override
    public AiReportResponse report(AiReportRequest request) {
        String type = request == null || request.getType() == null ? "student" : request.getType();
        type = type.trim().toLowerCase(Locale.ROOT);
        Integer clazzId = request == null ? null : request.getClazzId();
        ReportData data = aiDataService.loadReportData(type, clazzId);

        if (!aiClient.isLive()) {
            return new AiReportResponse(aiDataService.renderReport(data), "mock");
        }

        try {
            List<AiMessage> messages = List.of(
                    new AiMessage("system", aiDataService.buildReportPrompt(data)),
                    new AiMessage("user", "请根据以上数据生成报告。"));
            String markdown = aiClient.chat(messages);
            if (markdown == null || markdown.isBlank()) {
                return new AiReportResponse(aiDataService.renderReport(data)
                        + "\n\n> 真实模型暂时不可用，已回退到演示模式。", "mock");
            }
            return new AiReportResponse(markdown, "live");
        } catch (Exception e) {
            log.warn("AI 报告生成失败，回退演示模式：{}", e.getMessage());
            return new AiReportResponse(aiDataService.renderReport(data)
                    + "\n\n> 真实模型暂时不可用，已回退到演示模式。", "mock");
        }
    }

    private String mode() {
        return aiClient.isLive() ? "live" : "mock";
    }
}
