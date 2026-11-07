package com.itheima.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiReportResponse {

    private String markdown; // Markdown 报告
    private String mode;     // live / mock
}
