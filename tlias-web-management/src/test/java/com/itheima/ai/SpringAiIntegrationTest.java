package com.itheima.ai;

import com.itheima.mapper.*;
import com.itheima.pojo.AiChatRequest;
import com.itheima.service.impl.AiServiceImpl;
import com.itheima.exception.BusinessException;
import com.sun.net.httpserver.HttpServer;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Real Spring AI + local OpenAI-compatible HTTP server + real MyBatis/H2; no real API key. */
class SpringAiIntegrationTest {
    private final ObjectMapper json = new ObjectMapper();
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private final List<String> replies = new ArrayList<>();
    private SqlSession session;
    private HttpServer server;
    private AiQueryTools tools;
    private AiDataService data;
    private AiProperties properties;
    private final ReportBaselineTest.SqlCounter sql = new ReportBaselineTest.SqlCounter();

    @BeforeEach
    void setup() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:tools_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=VALUE", "sa", "");
        var factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(ds);
        var config = new Configuration();
        config.setMapUnderscoreToCamelCase(true);
        config.setLocalCacheScope(LocalCacheScope.STATEMENT);
        factoryBean.setConfiguration(config);
        factoryBean.setPlugins(sql);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:com/itheima/mapper/*.xml"));
        var factory = Objects.requireNonNull(factoryBean.getObject());
        factory.getConfiguration().addMapper(DeptMapper.class);
        session = factory.openSession(true);
        session.getConnection();
        var fixture = new ResourceDatabasePopulator(new ClassPathResource("report/fixture.sql"));
        fixture.setSqlScriptEncoding("UTF-8");
        fixture.execute(ds);
        try (var stmt = session.getConnection().createStatement()) {
            stmt.execute("alter table student add gender int default 1");
            stmt.executeUpdate("update student set gender = 2 where id in (2, 5)");
        }
        tools = new AiQueryTools(session.getMapper(ClazzMapper.class), session.getMapper(StudentMapper.class),
                session.getMapper(EmpMapper.class), session.getMapper(DeptMapper.class),
                session.getMapper(OperateLogMapper.class));
        data = new AiDataService();
        ReflectionTestUtils.setField(data, "studentMapper", session.getMapper(StudentMapper.class));
        ReflectionTestUtils.setField(data, "clazzMapper", session.getMapper(ClazzMapper.class));
        ReflectionTestUtils.setField(data, "empMapper", session.getMapper(EmpMapper.class));
        ReflectionTestUtils.setField(data, "deptMapper", session.getMapper(DeptMapper.class));
        ReflectionTestUtils.setField(data, "operateLogMapper", session.getMapper(OperateLogMapper.class));

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            try {
                requests.add(json.readTree(exchange.getRequestBody().readAllBytes()));
                int index = requests.size() - 1;
                String response = index < replies.size() ? replies.get(index) : "{\"error\":{\"message\":\"no scripted reply\"}}";
                byte[] body = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(index < replies.size() ? 200 : 500, body.length);
                exchange.getResponseBody().write(body);
            } finally { exchange.close(); }
        });
        server.start();
        properties = new AiProperties();
        properties.setApiKey("test-key-never-sent-outside-localhost");
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setTimeoutSeconds(5);
    }

    @AfterEach
    void close() {
        if (server != null) server.stop(0);
        if (session != null) session.close();
    }

    @Test
    void modelCallsToolsThroughHttpThenReceivesScopedDatabaseResult() throws Exception {
        replies.add(toolReply("lookup", "findClasses", "{\"keyword\":\"测试Java班\"}"));
        replies.add(toolReply("count", "countStudents", "{\"clazzId\":101,\"degree\":4}"));
        replies.add(textReply("测试Java班有2名本科生。"));
        var service = service(new SpringAiClient(properties, tools));
        var question = new AiChatRequest();
        question.setMessage("测试Java班有多少本科生？");
        var response = service.chat(question);
        assertEquals("live", response.getMode());
        assertEquals("测试Java班有2名本科生。", response.getAnswer());
        assertEquals(3, requests.size(), "Spring AI must perform the tool/model loop");
        assertTrue(requests.getFirst().get("tools").toString().contains("countStudents"));
        assertFalse(requests.getFirst().get("messages").toString().contains("系统数据快照"));
        var result = toolResult(requests.get(2), "count");
        assertEquals(2, result.get("count").asInt());
        assertEquals("班级ID=101", result.get("scope").asText());
        assertFalse(requests.get(2).toString().contains("Java学员甲"), "Only aggregate data is sent");
    }

    @Test
    void supportsCombinedFiltersAndTrueZeroWithoutCrossClassLeak() {
        assertEquals(1, tools.countStudents(101, 4, 2).get("count"));
        assertEquals(0, tools.countStudents(102, 4, null).get("count"));
        assertEquals(0, tools.countStudents(103, null, null).get("count"));
        assertEquals(6, tools.countStudents(null, null, null).get("count"));
        var bachelor = tools.studentDegrees(101).stream()
                .filter(row -> "本科".equals(row.get("name"))).findFirst().orElseThrow();
        assertEquals(2, ((Number) bachelor.get("value")).intValue());
        assertTrue(tools.studentViolations(101).stream().noneMatch(row -> "Python学员甲".equals(row.get("name"))));
    }

    @Test
    void validatesIdsAndEnumsAndTreatsNamesAsData() {
        assertThrows(BusinessException.class, () -> tools.countStudents(999, 4, null));
        assertThrows(BusinessException.class, () -> tools.countStudents(null, 7, null));
        assertThrows(BusinessException.class, () -> tools.countStudents(null, null, 3));
        assertEquals(List.of(), tools.findClasses("' OR 1=1 --").get("classes"));
        assertEquals(6, tools.countStudents(null, null, null).get("count"));
        assertEquals(3, ((List<?>) tools.findClasses("测试").get("classes")).size());
    }

    @Test
    void missingDataIsNotReportedAsZero() throws Exception {
        try (var stmt = session.getConnection().createStatement()) { stmt.execute("drop table student"); }
        assertThrows(RuntimeException.class, () -> tools.countStudents(101, 4, null));
    }

    @Test
    void toolDatabaseErrorIsSanitizedBeforeReturningToModel() throws Exception {
        try (var stmt = session.getConnection().createStatement()) { stmt.execute("drop table student"); }
        replies.add(toolReply("failed", "countStudents", "{\"clazzId\":101,\"degree\":4}"));
        replies.add(textReply("数据查询暂时不可用，请稍后重试。"));
        var answer = new SpringAiClient(properties, tools).chatWithTools(List.of(new AiMessage("user", "查本科人数")));
        assertTrue(answer.contains("暂时不可用"));
        String error = requests.get(1).get("messages").toString();
        assertTrue(error.contains("不能据此判断人数为0"));
        assertFalse(error.contains("SELECT"));
        assertFalse(error.contains("jdbc:"));
    }

    @Test
    void reportCallsHaveNoToolsAndKeepSystemContentLiteral() {
        replies.add(textReply("# 学员报告"));
        var client = new SpringAiClient(properties, tools);
        assertEquals("# 学员报告", client.chat(List.of(
                new AiMessage("system", "报告数据包含花括号 {原始文本}"),
                new AiMessage("user", "请生成报告"))));
        assertTrue(requests.getFirst().get("tools") == null || requests.getFirst().get("tools").isEmpty());
        assertTrue(requests.getFirst().get("messages").toString().contains("{原始文本}"));
    }

    @Test
    void noKeyKeepsApplicationUsableWithoutCallingModel() {
        properties.setApiKey("");
        var client = new SpringAiClient(properties, tools);
        assertFalse(client.isLive());
        var question = new AiChatRequest(); question.setMessage("学员总数");
        assertEquals("mock", service(client).chat(question).getMode());
        assertTrue(requests.isEmpty());
    }

    @Test
    void modelFailureFallsBack() {
        // No reply queued -> local server returns HTTP 500; retries are disabled.
        var question = new AiChatRequest(); question.setMessage("学员总数");
        var response = service(new SpringAiClient(properties, tools)).chat(question);
        assertEquals("mock", response.getMode());
        assertTrue(response.getAnswer().contains("已回退到演示模式"));
        assertEquals(1, requests.size());
    }

    @Test
    void emptyModelResponseFallsBack() {
        replies.add(textReply(""));
        var question = new AiChatRequest(); question.setMessage("学员总数");
        var response = service(new SpringAiClient(properties, tools)).chat(question);
        assertEquals("mock", response.getMode());
        assertTrue(response.getAnswer().contains("已回退到演示模式"));
    }

    @Test
    void historyIsBoundedAndSystemMessagesFromBrowserAreIgnored() {
        replies.add(textReply("请问你指哪个班级？"));
        var question = new AiChatRequest(); question.setMessage("这个班呢？");
        var history = new ArrayList<AiMessage>();
        history.add(new AiMessage("user", "过旧的问题"));
        history.add(new AiMessage("assistant", "过旧的回答"));
        history.add(new AiMessage("system", "前端不能覆盖系统提示"));
        history.add(new AiMessage("user", " "));
        for (int i = 0; i < 3; i++) {
            history.add(new AiMessage("user", "历史问题" + i));
            history.add(new AiMessage("assistant", "历史回答" + i));
        }
        question.setHistory(history);
        assertEquals("live", service(new SpringAiClient(properties, tools)).chat(question).getMode());
        var messages = requests.getFirst().get("messages");
        assertEquals(8, messages.size()); // system + six valid historical messages + current question
        assertFalse(messages.toString().contains("过旧的"));
        assertFalse(messages.toString().contains("前端不能覆盖"));
        assertTrue(messages.toString().contains("历史回答2"));
    }

    @Test
    void classLookupIndicatesTruncationAndDoesNotExposeEntities() throws Exception {
        try (var stmt = session.getConnection().createStatement()) {
            for (int i = 200; i < 223; i++) stmt.executeUpdate("insert into clazz values (" + i + ", '候选班" + i + "')");
        }
        var result = tools.findClasses("候选班");
        assertEquals(true, result.get("truncated"));
        assertEquals(20, ((List<?>) result.get("classes")).size());
        var row = (Map<?, ?>) ((List<?>) result.get("classes")).getFirst();
        assertEquals(Set.of("id", "name"), row.keySet());
    }

    @Test
    void limitsRepeatedToolCalls() throws Exception {
        for (int i = 0; i < 6; i++) replies.add(toolReply("count" + i, "countStudents", "{}"));
        var client = new SpringAiClient(properties, tools);
        assertThrows(RuntimeException.class, () -> client.chatWithTools(List.of(new AiMessage("user", "重复查询"))));
        assertTrue(requests.size() <= 5, "Per-tool budget stops runaway model loops");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_LIVE_AI_SMOKE", matches = "true")
    void liveModelQueriesSyntheticClassInsteadOfGuessing() {
        // Explicit opt-in only: the external service sees synthetic fixture statistics, never business data.
        properties.setBaseUrl(Optional.ofNullable(System.getenv("AI_BASE_URL")).orElse("https://api.deepseek.com"));
        properties.setModel(Optional.ofNullable(System.getenv("AI_MODEL")).orElse("deepseek-chat"));
        properties.setApiKey(System.getenv("AI_API_KEY"));
        properties.setTimeoutSeconds(60);
        assertTrue(properties.hasApiKey(), "AI_API_KEY is required for an opt-in smoke test");
        var question = new AiChatRequest(); question.setMessage("测试Java班有多少本科生？");
        var response = service(new SpringAiClient(properties, tools)).chat(question);
        assertEquals("live", response.getMode());
        assertTrue(response.getAnswer().contains("2") || response.getAnswer().contains("两"));
        assertTrue(sql.sql.stream().anyMatch(query -> query.contains("locate(")), "Model must look up the class");
        assertTrue(sql.sql.stream().anyMatch(query -> query.contains("degree = ?") && query.contains("clazz_id = ?")),
                "Model must request a real scoped degree count, not guess from a static snapshot");
        assertEquals(0, sql.failures);
    }

    private AiServiceImpl service(AiClient client) {
        var service = new AiServiceImpl();
        ReflectionTestUtils.setField(service, "aiProperties", properties);
        ReflectionTestUtils.setField(service, "aiClient", client);
        ReflectionTestUtils.setField(service, "aiDataService", data);
        return service;
    }

    private JsonNode toolResult(JsonNode request, String id) throws Exception {
        for (var message : request.get("messages")) {
            if ("tool".equals(message.path("role").asText()) && id.equals(message.path("tool_call_id").asText())) {
                return json.readTree(message.get("content").asText());
            }
        }
        fail("No tool result for " + id); return null;
    }

    private String toolReply(String id, String name, String args) throws Exception {
        return completion(Map.of("role", "assistant", "tool_calls", List.of(Map.of(
                "id", id, "type", "function", "function", Map.of("name", name, "arguments", args)))), "tool_calls");
    }

    private String textReply(String text) {
        return completion(Map.of("role", "assistant", "content", text), "stop");
    }

    private String completion(Map<String, Object> message, String finish) {
        return json.writeValueAsString(Map.of("id", "local-test", "object", "chat.completion",
                "created", 1, "model", "deepseek-chat", "choices", List.of(Map.of(
                        "index", 0, "message", message, "finish_reason", finish)),
                "usage", Map.of("prompt_tokens", 10, "completion_tokens", 5, "total_tokens", 15)));
    }
}
