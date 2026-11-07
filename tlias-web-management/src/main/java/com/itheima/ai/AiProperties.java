package com.itheima.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 模块配置。
 * 默认走 DeepSeek 的 OpenAI 兼容接口；api-key 为空时自动使用演示模式。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private double temperature = 0.3;
    private int timeoutSeconds = 60;

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
