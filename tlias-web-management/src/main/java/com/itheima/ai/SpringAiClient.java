package com.itheima.ai;

import com.itheima.exception.BusinessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolCallLimitExceededException;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** 协议编解码和工具执行循环由 Spring AI 负责，保留业务层的演示模式边界。 */
@Component
public class SpringAiClient implements AiClient {
    private final ChatClient client;
    private final AiQueryTools tools;

    public SpringAiClient(AiProperties properties, AiQueryTools tools) {
        this.tools = tools;
        if (!properties.hasApiKey()) {
            this.client = null;
            return;
        }
        var model = OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(properties.getBaseUrl().replaceAll("/+$", ""))
                        .apiKey(properties.getApiKey())
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .timeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())))
                        .maxRetries(0)
                        .build())
                .build();
        var manager = ToolCallingManager.builder().maxCallsPerTool(4).maxTotalToolCalls(12)
                .toolExecutionExceptionProcessor(error -> error.getCause() instanceof BusinessException business
                        ? business.getMessage()
                        : "数据查询暂时不可用，不能据此判断人数为0；请稍后重试。")
                .build();
        this.client = ChatClient.builder(model)
                .defaultAdvisors(ToolCallingAdvisor.builder().toolCallingManager(manager).build())
                .build();
    }

    @Override
    public boolean isLive() {
        return client != null;
    }

    @Override
    public String chat(List<AiMessage> messages) {
        return invoke(messages, false);
    }

    @Override
    public String chatWithTools(List<AiMessage> messages) {
        return invoke(messages, true);
    }

    private String invoke(List<AiMessage> messages, boolean enableTools) {
        if (!isLive()) throw new IllegalStateException("未配置 AI API Key");
        List<Message> input = messages.stream().map(this::toMessage).toList();
        var request = client.prompt().messages(input);
        if (enableTools) request.tools(tools);
        var response = request.call().chatResponse();
        if (response == null || response.getResult() == null) throw new IllegalStateException("模型返回内容为空");
        if (ToolCallLimitExceededException.FINISH_REASON.equals(response.getResult().getMetadata().getFinishReason())) {
            throw new IllegalStateException("本轮查询次数超限，请缩小问题范围");
        }
        String answer = response.getResult().getOutput().getText();
        if (answer == null || answer.isBlank()) throw new IllegalStateException("模型返回内容为空");
        return answer;
    }

    private Message toMessage(AiMessage message) {
        return switch (message.getRole()) {
            case "system" -> new SystemMessage(message.getContent());
            case "user" -> new UserMessage(message.getContent());
            case "assistant" -> new AssistantMessage(message.getContent());
            default -> throw new IllegalArgumentException("不支持的消息角色");
        };
    }
}
