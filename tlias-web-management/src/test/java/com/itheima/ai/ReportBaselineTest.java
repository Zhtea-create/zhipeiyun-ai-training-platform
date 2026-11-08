package com.itheima.ai;

import com.itheima.mapper.*;
import com.itheima.pojo.AiReportRequest;
import com.itheima.pojo.AiReportResponse;
import com.itheima.service.impl.AiServiceImpl;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Acceptance tests after refactoring; original measurements live in docs/report-baseline-before. */
class ReportBaselineTest {
    private static final List<String> measurements = new ArrayList<>();
    private SqlSession session;
    private final SqlCounter counter = new SqlCounter();
    private final ModelStub model = new ModelStub();
    private AiServiceImpl service;

    @BeforeEach
    void setup() throws Exception {
        // No Spring Boot context: never reads application.yml, API keys or the real MySQL URL.
        var ds = new DriverManagerDataSource("jdbc:h2:mem:report_" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=VALUE", "sa", "");
        var factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(ds);
        var configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        // Production service has no transaction; each mapper call uses its own session there.
        // Disable session cache here so a shared test session cannot hide repeated SQL.
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
        configuration.setCacheEnabled(false);
        factoryBean.setConfiguration(configuration);
        factoryBean.setPlugins(counter);
        factoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:com/itheima/mapper/*.xml"));
        var factory = Objects.requireNonNull(factoryBean.getObject());
        factory.getConfiguration().addMapper(DeptMapper.class);
        session = factory.openSession(true);
        // Keep this connection alive while initializing the same in-memory database.
        session.getConnection();
        var fixtures = new ResourceDatabasePopulator(new ClassPathResource("report/fixture.sql"));
        fixtures.setSqlScriptEncoding("UTF-8");
        fixtures.execute(ds);

        var data = new AiDataService();
        ReflectionTestUtils.setField(data, "empMapper", session.getMapper(EmpMapper.class));
        ReflectionTestUtils.setField(data, "studentMapper", session.getMapper(StudentMapper.class));
        ReflectionTestUtils.setField(data, "clazzMapper", session.getMapper(ClazzMapper.class));
        ReflectionTestUtils.setField(data, "deptMapper", session.getMapper(DeptMapper.class));
        ReflectionTestUtils.setField(data, "operateLogMapper", session.getMapper(OperateLogMapper.class));
        service = new AiServiceImpl();
        ReflectionTestUtils.setField(service, "aiProperties", new AiProperties());
        ReflectionTestUtils.setField(service, "aiClient", model);
        ReflectionTestUtils.setField(service, "aiDataService", data);
    }

    @AfterEach
    void cleanup() {
        if (session != null) session.close();
    }

    @ParameterizedTest(name = "{0}-{1}-class{2}: {3} SQL")
    @CsvSource({
        "student, OFFLINE, 0, 5",
        "student, SUCCESS, 0, 5",
        "student, FAILURE, 0, 5",
        "student, EMPTY, 0, 5",
        "student, NULL, 0, 5",
        "student, OFFLINE, 101, 4",
        "student, SUCCESS, 101, 4",
        "student, FAILURE, 101, 4",
        "student, EMPTY, 101, 4",
        "student, SUCCESS, 102, 4",
        "student, OFFLINE, 102, 4",
        "student, SUCCESS, 103, 4",
        "student, FAILURE, 103, 4",
        "employee, OFFLINE, 0, 4",
        "employee, SUCCESS, 0, 4",
        "employee, FAILURE, 0, 4",
        "employee, EMPTY, 0, 4"
    })
    void verifiesScopedReportAndQueryBudget(String type, String behavior, int clazzId, int expectedSql) throws Exception {
        model.behavior = behavior;
        AiReportRequest request = request(type, clazzId);
        AiReportResponse response = service.report(request);
        assertEquals(0, counter.failures, "A swallowed SQL error must fail the baseline test");
        assertEquals(expectedSql, counter.sql.size());
        assertEquals(behavior.equals("SUCCESS") ? "live" : "mock", response.getMode());
        assertEquals(behavior.equals("OFFLINE") ? 0 : 1, model.calls);

        if (!behavior.equals("OFFLINE")) {
            String prompt = model.messages.getFirst().getContent();
            assertEquals("system", model.messages.getFirst().getRole());
            assertReportScope(type, clazzId, prompt);
        }
        if (!behavior.equals("SUCCESS")) {
            assertReportScope(type, clazzId, response.getMarkdown());
        }
        if (List.of("FAILURE", "EMPTY", "NULL").contains(behavior)) {
            assertTrue(response.getMarkdown().contains("已回退到演示模式"));
        }
        assertTrue(counter.sql.stream().noneMatch(sql -> sql.contains("operate_log")));
        String name = type + "-" + behavior.toLowerCase(Locale.ROOT) + "-" + clazzId;
        record(name, response);
    }

    @Test
    void fixtureHasIndependentExpectedStatistics() {
        var students = session.getMapper(StudentMapper.class);
        var classes = session.getMapper(ClazzMapper.class);
        assertEquals(6, students.countAll());
        assertEquals(4, students.countByClazzId(101));
        assertEquals(2, students.countByClazzId(102));
        assertEquals(0, students.countByClazzId(103));
        assertEquals(3, classes.countAll());
        var degrees = students.countStudentDegreeData();
        assertEquals(4, degrees.size());
        assertEquals(2, ((Number) degrees.stream().filter(x -> "本科".equals(x.get("name")))
                .findFirst().orElseThrow().get("value")).intValue());
        assertEquals("Python学员甲", students.topViolation(5).getFirst().get("name"));
        assertEquals(0, counter.failures);
    }

    @Test
    void rejectsMissingClassBeforeCallingModel() {
        model.behavior = "SUCCESS";
        var error = assertThrows(com.itheima.exception.BusinessException.class,
                () -> service.report(request("student", 999)));
        assertEquals("班级不存在", error.getMessage());
        assertEquals(1, counter.sql.size());
        assertEquals(0, model.calls);
    }

    @Test
    void emptyClassHasNoOtherClassData() throws Exception {
        var response = service.report(request("student", 103));
        assertReportScope("student", 103, response.getMarkdown());
        assertEquals(4, counter.sql.size());
        record("student-offline-103", response);
    }

    @Test
    void employeeIgnoresClassAndNormalizesType() {
        var response = service.report(request(" Employee ", 999));
        assertReportScope("employee", 0, response.getMarkdown());
        assertEquals(4, counter.sql.size());
    }

    @Test
    void rejectsUnsupportedTypeWithoutQuerying() {
        assertThrows(com.itheima.exception.BusinessException.class,
                () -> service.report(request("unknown", 0)));
        assertEquals(0, counter.sql.size());
    }

    @Test
    void nullRequestDefaultsToGlobalStudentReport() {
        assertReportScope("student", 0, service.report(null).getMarkdown());
        assertEquals(5, counter.sql.size());
    }

    @Test
    void modelFailureReusesAlreadyReadData() throws Exception {
        model.behavior = "FAILURE";
        model.beforeFailure = () -> {
            // Simulate a concurrent write after data preparation and before model failure.
            try (var statement = session.getConnection().createStatement()) {
                statement.executeUpdate("delete from student where clazz_id = 101");
            } catch (java.sql.SQLException error) { throw new IllegalStateException(error); }
        };
        var response = service.report(request("student", 101));
        assertReportScope("student", 101, response.getMarkdown());
        assertEquals(4, counter.sql.size());
        assertTrue(model.messages.getFirst().getContent().contains("学员总数：4 人"));
    }

    @Test
    void filtersClassBeforeSelectingTopViolations() throws Exception {
        try (var statement = session.getConnection().createStatement()) {
            for (int id = 10; id < 16; id++) {
                statement.executeUpdate("insert into student values (" + id
                        + ", '外班高扣分学员', 102, 4, 10, 100)");
            }
        }
        model.behavior = "SUCCESS";
        service.report(request("student", 101));
        String prompt = model.messages.getFirst().getContent();
        assertTrue(prompt.contains("Java学员乙"));
        assertTrue(prompt.contains("Java学员丙"));
        assertFalse(prompt.contains("外班高扣分学员"));
        assertEquals(4, counter.sql.size());
    }

    @Test
    void databaseFailureCannotMasqueradeAsZeroStudentReport() throws Exception {
        try (var statement = session.getConnection().createStatement()) {
            statement.execute("drop table student");
        }
        model.behavior = "SUCCESS";
        assertThrows(RuntimeException.class, () -> service.report(request("student", 0)));
        assertEquals(0, model.calls);
    }

    private static void assertReportScope(String type, int clazzId, String text) {
        assertFalse(text.contains("NaN"));
        assertFalse(text.contains("Infinity"));
        if (type.equals("employee")) {
            assertTrue(text.contains("员工总数：4 人"));
            assertTrue(text.contains("部门数量：2 个"));
            assertTrue(text.contains("| 讲师 | 2 |"));
            assertFalse(text.contains("学员"));
            assertFalse(text.contains("测试Java班"));
            return;
        }
        assertFalse(text.contains("员工总数"));
        if (clazzId == 101) {
            assertTrue(text.contains("学员总数：4 人"));
            assertTrue(text.contains("班级数量：1 个"));
            assertTrue(text.contains("| 本科 | 2 | 50.0% |"));
            assertTrue(text.contains("| 大专 | 1 | 25.0% |"));
            assertTrue(text.contains("| 硕士 | 1 | 25.0% |"));
            assertTrue(text.contains("Java学员丙"));
            assertFalse(text.contains("Python"));
            assertFalse(text.contains("测试空班"));
        } else if (clazzId == 102) {
            assertTrue(text.contains("学员总数：2 人"));
            assertTrue(text.contains("| 高中 | 1 | 50.0% |"));
            assertTrue(text.contains("| 大专 | 1 | 50.0% |"));
            assertTrue(text.contains("Python学员甲"));
            assertFalse(text.contains("Java"));
        } else if (clazzId == 103) {
            assertTrue(text.contains("学员总数：0 人"));
            assertTrue(text.contains("班级数量：1 个"));
            assertTrue(text.contains("聚焦班级：测试空班（0 人）"));
            assertFalse(text.contains("Java"));
            assertFalse(text.contains("Python"));
        } else {
            assertTrue(text.contains("学员总数：6 人"));
            assertTrue(text.contains("班级数量：3 个"));
            assertTrue(text.contains("测试Java班"));
            assertTrue(text.contains("测试Python班"));
        }
    }

    private static AiReportRequest request(String type, int clazzId) {
        var request = new AiReportRequest();
        request.setType(type);
        if (clazzId != 0) request.setClazzId(clazzId);
        return request;
    }

    private void record(String name, AiReportResponse response) throws Exception {
        Path dir = Path.of("target/report-after");
        Files.createDirectories(dir);
        String artifact = "# " + name + "\n\nMode: " + response.getMode()
                + "\n\nSQL count: " + counter.sql.size() + "\n\n## SQL\n\n"
                + String.join("\n\n", counter.sql) + "\n\n## Response\n\n" + response.getMarkdown();
        if (model.messages != null) artifact += "\n\n## Model input\n\n" + model.messages.getFirst().getContent();
        Files.writeString(dir.resolve(name + ".md"), artifact, StandardCharsets.UTF_8);
        measurements.add("| " + name + " | " + counter.sql.size() + " | " + model.calls + " |");
    }

    @AfterAll
    static void summary() throws Exception {
        Collections.sort(measurements);
        Files.createDirectories(Path.of("target/report-after"));
        Files.writeString(Path.of("target/report-after/summary.md"),
                "# After refactor\n\nH2 fixture; actual MyBatis prepared SQL; model stub; no HTTP/AOP.\n\n"
                        + "| Case | SQL count | Model calls |\n|---|---:|---:|\n"
                        + String.join("\n", measurements) + "\n", StandardCharsets.UTF_8);
    }

    private static final class ModelStub implements AiClient {
        String behavior = "OFFLINE";
        int calls;
        List<AiMessage> messages;
        Runnable beforeFailure = () -> {};
        public boolean isLive() { return !behavior.equals("OFFLINE"); }
        public String chat(List<AiMessage> input) {
            calls++;
            messages = List.copyOf(input);
            if (behavior.equals("FAILURE")) {
                beforeFailure.run();
                throw new IllegalStateException("simulated model failure");
            }
            if (behavior.equals("NULL")) return null;
            return behavior.equals("EMPTY") ? "" : "# 固定模型响应\n\n用于验证报告调用流程。";
        }
    }

    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare",
            args = {Connection.class, Integer.class}))
    public static final class SqlCounter implements Interceptor {
        final List<String> sql = new ArrayList<>();
        int failures;
        public Object intercept(Invocation invocation) throws Throwable {
            sql.add(((StatementHandler) invocation.getTarget()).getBoundSql().getSql()
                    .replaceAll("\\s+", " ").trim());
            try { return invocation.proceed(); }
            catch (Throwable error) { failures++; throw error; }
        }
    }
}
