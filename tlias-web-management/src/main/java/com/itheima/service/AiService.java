package com.itheima.service;

import com.itheima.pojo.AiChatRequest;
import com.itheima.pojo.AiChatResponse;
import com.itheima.pojo.AiReportRequest;
import com.itheima.pojo.AiReportResponse;
import com.itheima.pojo.AiStatus;

public interface AiService {

    AiStatus status();

    AiChatResponse chat(AiChatRequest request);

    AiReportResponse report(AiReportRequest request);
}
