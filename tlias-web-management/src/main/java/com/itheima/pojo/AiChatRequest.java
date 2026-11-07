package com.itheima.pojo;

import com.itheima.ai.AiMessage;
import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {

    private String message;          // 当前问题
    private List<AiMessage> history; // 历史对话（前端保存，后端无状态）
}
