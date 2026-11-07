package com.itheima.ai;

import java.util.List;

public interface AiClient {

    // 是否配置了真实模型
    boolean isLive();

    // 调用大模型，返回回答文本
    String chat(List<AiMessage> messages);
}
