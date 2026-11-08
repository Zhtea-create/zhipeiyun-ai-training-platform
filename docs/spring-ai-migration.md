# Spring AI 迁移说明

## 范围

当前后端使用 Spring Boot 4.0.8、Spring AI 2.0.2。采用官方 `spring-ai-openai` 和 `spring-ai-client-chat` 模块，通过 BOM 对齐版本。显式配置模型是为了保留现有 `ai.*` 配置，以及没有 API Key 时仍能启动的演示模式；协议编解码、工具 schema 和工具执行循环均由框架负责。

已移除旧 `OpenAiCompatibleClient`、无人调用的全局 `buildSnapshot()`，以及旧的 `buildStudentReport(Integer)` / `buildEmployeeReport()` 入口。报告统一使用 `loadReportData` → `buildReportPrompt` / `renderReport`，避免恢复重复查询路径。仍保留 `AiClient` 作为业务与模型的接口、`AiMessage` 作为前端历史消息 DTO，以及当前无 Key / 模型失败时实际使用的演示回答和报告模板。

## 调用路径

```text
聊天页面 → POST /ai/chat → AiServiceImpl
    → SpringAiClient.chatWithTools → Spring AI ChatClient
    → 模型请求工具 → AiQueryTools → MyBatis → 数据库
    → Spring AI 将工具结果发回模型 → 最终回答

报告页面 → POST /ai/report → loadReportData(type, clazzId)
    → buildReportPrompt(data) → SpringAiClient.chat（无工具）
    → 成功返回报告，失败使用原 data 渲染备用报告
```

例如“测试Java班有多少本科生”，可调用 `findClasses("测试Java班")`，取得候选班级 ID，再调用 `countStudents(101, 4, null)`。数据库按班级与学历实际过滤，不再用全校学历人数推断班内人数。

## 配置与兼容

现有 application.yml 的 `ai.base-url`、`ai.api-key`、`ai.model`、`ai.temperature`、`ai.timeout-seconds` 继续有效，无需改写本机配置或暴露 Key。`AI_API_KEY` 等环境变量继续通过原配置占位符读取。

默认 DeepSeek 根地址为 `https://api.deepseek.com`；其他兼容服务如要求 `/v1`，需在根地址中包含它。SDK 在根地址后添加 `/chat/completions`。聊天模型必须支持 function/tool calling；仅支持文本的兼容接口不能完成动态查询，会沿用模型调用失败时的演示降级。

HTTP 超时沿用现有配置（最少5秒），自动重试设为0，避免多轮工具调用放大等待时间。工具执行使用 Spring AI 的预算限制：同一工具最多4次、总共12次，超限不把框架诊断当作成功回答，而交由业务层处理降级。

## 数据范围

工具仅开放只读的参数化查询与聚合；不允许模型提供 SQL、执行任意方法或修改数据。班级候选只返回 ID 与名称，最多20个并标明是否截断。人数工具支持班级、学历、性别的组合筛选；不存在的班级、非法学历/性别不会作为0人处理。违纪排名先按班级过滤，再取前5名。

员工统计只返回聚合值；操作日志只返回最近5条的时间、方法和耗时，不返回完整参数和结果。工具异常由 Spring AI 的异常处理器转换为可读提示，不把数据库 SQL 错误详情发给模型，也不转换成0人。必要摘要仍可能包含班级名与违纪学员姓名。当前沿用系统所有登录员工同权的权限范围；未来引入角色或机构隔离时，需要在查询工具层落实访问范围。

班级名称歧义时由提示词要求模型澄清；这是模型行为约束，不是确定性保证，真实模型验收必须覆盖重名、别称和多候选场景。历史消息继续沿用前端传入的最近8条有效范围，不新增共享的全局模型记忆，避免不同用户混用上下文。

## 验证方式

```powershell
$env:JAVA_HOME='D:/Java/JDK'
mvn -Dtest=ReportBaselineTest,SpringAiIntegrationTest test
```

在 `tlias-web-management` 中运行。测试不启动完整业务应用，不读取真实数据库连接或 Key。使用隔离 H2、生产 MyBatis SQL、本机 OpenAI 兼容模拟服务和真实 Spring AI 客户端，验证多轮工具调用的协议链路、班级/学历/性别组合筛选、真正的0人、空班、非法参数、数据库故障、工具调用上限、空回答、模型失败与历史消息边界。

报告原有26项验收继续覆盖5条/4条 SQL 预算、班级隔离及降级复用。模拟服务的工具选择和最终文本是预设的，不能证明真实模型的自然语言识别准确率。另有默认跳过的真实模型测试：设置 `RUN_LIVE_AI_SMOKE=true` 和 `AI_API_KEY` 后运行 `mvn -Dtest=SpringAiIntegrationTest#liveModelQueriesSyntheticClassInsteadOfGuessing test`，仅发送虚构测试数据，会调用外部模型并产生少量费用。

2026-11-08 已使用已配置的 DeepSeek 完成一次真实验证：询问“测试Java班有多少本科生”，返回2人，并通过 SQL 拦截器确认模型实际触发班级查找和带班级、学历条件的查询。该结果只代表此单个问题通过，不代表所有问法都正确。进一步验收问题：

- 使用实际班级全名询问本科人数，对照学员管理筛选结果。
- 询问同班本科女生人数，检查多个条件同时生效。
- 使用“一班”等不明确名称，确认助手请求澄清。
- 询问存在但没有学员的班级，确认0人而不是全校数据。
- 切换聊天页面、继续追问，确认原有前端历史行为不变。
- 生成全校与单班报告，确认报告没有因工具调用新增查询。

构建与启动后端沿用 README；本次不自动停止或替换本机正在运行的服务，已运行的旧进程需重启才能加载新实现。前端只调整功能说明文案，构建命令仍为 `npm run build`。
