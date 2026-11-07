package com.itheima.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一条对话消息（role: system / user / assistant）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMessage {

    private String role;
    private String content;
}
