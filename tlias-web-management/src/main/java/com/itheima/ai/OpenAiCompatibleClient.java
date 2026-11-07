package com.itheima.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议客户端。
 * DeepSeek、通义千问、Kimi、本地 vLLM 等都使用 /chat/completions 这一套请求格式。
 */
@Slf4j
@Component
public class OpenAiCompatibleClient implements AiClient {

    private final AiProperties properties;
    private final RestClient restClient;

    public OpenAiCompatibleClient(AiProperties properties) {
        this.properties = properties;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(Math.max(5, properties.getTimeoutSeconds())));

        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl().replaceAll("/+$", ""))
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean isLive() {
        return properties.hasApiKey();
    }

    @Override
    public String chat(List<AiMessage> messages) {
        if (!isLive()) {
            throw new IllegalStateException("未配置 AI API Key");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("temperature", properties.getTemperature());
        body.put("messages", messages.stream()
                .map(message -> Map.of("role", message.getRole(), "content", message.getContent()))
                .toList());

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            String content = extractContent(response);
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("模型返回内容为空");
            }
            return content;
        } catch (Exception e) {
            log.warn("调用大模型失败：{}", e.getMessage());
            throw new IllegalStateException("调用大模型失败：" + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object choicesValue = response.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return null;
        }
        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> messageMap)) {
            return null;
        }
        Object content = messageMap.get("content");
        return content instanceof String text ? text : null;
    }
}
