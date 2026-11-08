package com.itheima.ai;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** One report request's data; renderers must not query the database again. */
public record ReportData(String type, int total, int unitCount, String clazzName,
                         List<Map<String, Object>> classes,
                         List<Map<String, Object>> degrees,
                         List<Map<String, Object>> violations,
                         List<Map<String, Object>> jobs,
                         List<Map<String, Object>> genders,
                         LocalDateTime generatedAt) {
}
