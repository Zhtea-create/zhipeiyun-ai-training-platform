package com.itheima.ai;

import java.util.List;

public interface AiClient {

    // 是否配置了真实模型
    boolean isLive();

    // 调用大模型，返回回答文本
    String chat(List<AiMessage> messages);

    // 聊天可使用业务查询工具；报告仍只使用已准备好的数据。
    default String chatWithTools(List<AiMessage> messages) {
        return chat(messages);
    }
}
