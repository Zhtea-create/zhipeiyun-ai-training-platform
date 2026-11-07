# 智培云 · 智能培训管理平台

基于原黑马培训管理项目重构的版本：

- **后端**：Spring Boot 4 + MyBatis + MySQL + JWT，补全了学员统计、操作日志与 AI 模块
- **前端**：全新重构的 Vue 3 + Vite + Element Plus 管理后台（`tlias-frontend/`）
- **AI**：支持 OpenAI 兼容协议（DeepSeek / 通义 / Kimi / 本地 vLLM 等），默认无 Key 时使用**演示模式**

旧版前端源码已归档（不在仓库中），新前端在 `tlias-frontend/` 中。

---

## 目录结构

```
web-project02/
├── tlias-web-management/          # Spring Boot 后端
│   └── src/main/resources/db/
│       └── operate_log.sql        # 操作日志表 DDL
├── tlias-frontend/                # 新版前端（Vue 3 + Vite）
├── nginx/                         # nginx 运行目录 + 前端构建产物
└── Web开发-AI.openapi.json        # 接口文档
```

---

## 一、环境要求

| 工具 | 版本 |
| --- | --- |
| JDK | 25（`java -version` 必须是 25，PATH 里的 Java 8 不能用于运行） |
| Maven | 3.9+ |
| MySQL | 8.x |
| Node.js | 20.19+ / 22.12+（推荐 22+） |
| npm | 10+ |

---

## 二、数据库

1. 确保 MySQL 中存在 `tlias` 库（原项目的数据）。
2. 执行操作日志建表脚本：

```bash
mysql -uroot -p tlias < tlias-web-management/src/main/resources/db/operate_log.sql
```

3. 后端数据库连接在 `tlias-web-management/src/main/resources/application.yml` 中配置（该文件已被 git 忽略，请自行填写本机密码）。

> 日志模块依赖 `operate_log` 表；不建表时 `/log/page` 和 AI 模块的日志快照会降级为空。

---

## 三、运行后端

### 方式 A：Maven 直接运行

```bash
cd tlias-web-management
mvn spring-boot:run
```

### 方式 B：打包后运行（推荐，避免 Maven 残留进程）

```bash
cd tlias-web-management
mvn -DskipTests package
"D:/Java/JDK/bin/java" -jar target/tlias-web-management-0.0.1-SNAPSHOT.jar
```

后端地址：`http://localhost:8080`

> Windows 上 `mvn spring-boot:run` 被中断时可能留下占用 8080 的 Java 进程，可用 `netstat -ano | findstr :8080` 找到 PID 后结束。

---

## 四、运行前端

### 开发模式（热更新）

```bash
cd tlias-frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。Vite 会把 `/api` 代理到 `http://localhost:8080`。

### 生产构建

```bash
cd tlias-frontend
npm run build
```

构建结果直接输出到 `nginx/html/`（已在 `vite.config.js` 中配置）。

### 使用 nginx 部署

```bash
cd nginx
start nginx.exe        # Windows PowerShell/CMD 中启动
```

访问 `http://localhost:90`。

停止 nginx：

```bash
nginx.exe -s stop
```

nginx 配置：`nginx/conf/nginx.conf`

- `listen 90`
- `/` → `nginx/html`，SPA 路由回落到 `index.html`
- `/api/` → 反向代理到 `http://localhost:8080`

---

## 五、登录账号

数据库中的测试账号（密码均为 `123456`）：

| 用户名 | 姓名 |
| --- | --- |
| guansheng | 关胜 |
| linchong | 林冲 |
| qinming | 秦明 |
| huarong | 花荣 |

> 接口文档示例里的 `songjiang / 123456` 在当前数据库中不存在，请使用上表中的账号。

---

## 六、AI 模块

### 默认：演示模式（不需要 Key）

不配置任何 Key 时，`GET /ai/status` 返回 `mode = "mock"`：

- AI 助手会根据关键词，从数据库实时聚合数据后生成回答
- AI 报告会使用真实统计数字生成 Markdown 报告
- 前端会显示「演示模式」徽标

### 接入真实模型

后端按 **OpenAI 兼容协议** 调用 `POST {base-url}/chat/completions`。推荐 DeepSeek：

```powershell
# PowerShell（当前终端会话有效）
$env:AI_API_KEY="sk-你的Key"
$env:AI_BASE_URL="https://api.deepseek.com"
$env:AI_MODEL="deepseek-chat"
```

或在 IntelliJ IDEA 的 Run Configuration → Environment variables 中配置：

```
AI_API_KEY=sk-你的Key
AI_BASE_URL=https://api.deepseek.com
AI_MODEL=deepseek-chat
```

重启后端后，`/ai/status` 会返回 `mode = "live"`。

其他 OpenAI 兼容服务（通义千问 DashScope、Kimi、智谱、本地 vLLM）只需替换 `AI_BASE_URL` 和 `AI_MODEL`。

> Key 只从环境变量读取，不会进入前端代码或 git 仓库。真实模型调用失败时，后端会自动回退到演示模式，并在回答末尾提示。

### AI 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/ai/status` | 返回 `{mode, model}` |
| POST | `/ai/chat` | 请求 `{message, history}`，返回 `{answer, mode}` |
| POST | `/ai/report` | 请求 `{type, clazzId?, begin?, end?}`，返回 `{markdown, mode}` |

AI 只会读取聚合后的统计数据，不会把密码、身份证号、手机号等敏感字段发送给模型。

---

## 七、已实现的接口

| 模块 | 接口 |
| --- | --- |
| 登录 | `POST /login`（JWT，请求头 `token`） |
| 部门 | `GET/POST/PUT/DELETE /depts`、`GET /depts/{id}` |
| 员工 | `GET /emps`、`POST/PUT/DELETE /emps`、`GET /emps/list`、`GET /emps/{id}` |
| 班级 | `GET /clazzs`、`GET /clazzs/list`、`POST/PUT/DELETE /clazzs`、`GET /clazzs/{id}` |
| 学员 | `GET /students`、`POST/PUT/DELETE /students`、`GET /students/{id}`、`PUT /students/violation/{id}/{score}` |
| 报表 | `GET /report/empJobData`、`/empGenderData`、`/studentDegreeData`、`/studentCountData` |
| 日志 | `GET /log/page`（AOP 自动记录新增/修改/删除/AI 调用） |
| AI | `GET /ai/status`、`POST /ai/chat`、`POST /ai/report` |
| 上传 | `POST /upload`（阿里云 OSS，需配置 `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET`） |

---

## 八、已验证内容

- 后端编译、启动，登录与 401 鉴权
- 4 个报表接口返回真实数据
- 操作日志：修改部门后 `/log/page` 自动出现记录（操作人、类名、方法、参数、耗时）
- AI 演示模式：问答与报告均基于真实数据
- AI 真实模式：用本地模拟 OpenAI 服务验证过 `live` 链路
- 前端 10 个页面浏览器访问无控制台报错
- 部门新增 / 编辑 / 删除的浏览器端到端测试通过
- nginx 生产构建：`http://localhost:90` 登录、`/api` 代理、SPA 路由回落均正常

---

## 九、后续可做的加固

当前项目保留了原有测试数据与实现方式，以下内容属于可选加固，不影响功能：

- 员工密码改为 BCrypt 哈希（现有数据是明文 `123456`）
- JWT 密钥改为环境变量
- 为员工、班级、学员表单增加后端参数校验（`spring-boot-starter-validation`）
- 上传接口限制文件类型与大小（前端已限制，后端可再加一层）
- 为 AI 对话增加会话持久化与流式输出（SSE）
