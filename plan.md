DocVec_RAG
这是一个前后端分离的 RAG 项目，根目录以文档和约束文件为主：(README.MD:1、ARCHITECTURE.md:1、AGENTS.md:1、API_CONTRACT.md:1)。后端是独立的 Spring Boot/Maven 工程，入口在 DocVecRagApplication.java:1，配置在 application.yml:1，核心围绕会话级运行时配置、分块、embedding、存储回退等服务展开。前端是一个静态 Vue 页面，入口分别是 index.html:1 和 app.js:1，主要负责上传、参数配置和聊天交互，不承担业务编排。

Web_intelligent_function_navigation
这个项目是一个 Node + Crawlee + Playwright 的爬虫/站点导航器。真正的执行入口是 main.js:1，它负责抓取、提取页面可交互元素，并把结果写到 sitemap 数据文件；package.json 里也明确把启动命令指向了 node main.js，所以 package.json:1 是它的运行声明。server.js 则是 Web 管理层，提供 /api/status、/api/results、/api/datasets、/api/crawl/start 这类接口，并把静态页面从 public/ 提供出去。database/ 里放的是抓取产物 JSON，例如 database/sitemap.json；outputs/ 里放导出的结果文件，例如 outputs/crawl-report.html。整体上它是“爬虫核心 + Web 控制台 + 数据产物目录”的结构。

1. 已有基础

· 组件 A：网页爬取 & 输出工具
已经能爬取一个网站的各个链接，并把链接整理成 .js 文件。
这部分定位是内容采集，产出的是结构化的链接数据（可能还包含页面里的文本）。
· 组件 B：轻量 RAG 知识库问答应用
技术栈：Vue 3 前端 + Java Spring Boot 后端 + Cloudflare Vectorize / R2。
核心能力：支持上传文档 → 构建知识库 → 用户提问 → 检索增强生成答案。
特点：体积轻、可部署，接口初步跑通，强调可解释结果和可替换架构。

最终想做的系统，核心是 “能嵌入任何网页客户端” + “自动抓取该客户端的页面/链接内容” + “让用户用对话直接问出某个功能在哪个页面/位置”。

2. 想要的新系统（A + B 融合）

关键目标关键字：“很容易插入到现有网页客户端”。
也就是说，这个合并后的系统，理想形态应该像一个可嵌入的智能问答插件，而不是一个需要用户离开当前系统才能使用的独立应用。

3. 系统规划（定稿）

3.1 目标拆解
· 终端形态：嵌入式插件（Widget/SDK） + 后端服务；管理后台 P1 再做
· 能力闭环：采集 -> 索引 -> 问答 -> 定位 -> 复核（可追溯证据）
· 交付要求：1-2 行脚本接入、默认只读、低侵入、低运维成本

3.2 功能边界与范围
· 站点采集边界：同域/白名单域名；路径可配置；不做跨站索引聚合
· 权限边界：默认不携带登录态；需管理员显式授权才可抓取登录页
· 问答范围：功能入口与页面定位，不承诺复杂业务策略解读
· 结果可信度：必须返回证据（来源页面+元素文本/上下文）和可点击跳转
· 安全边界：禁止触发提交/支付/删除等危险操作

3.3 系统模块规划
· 嵌入式前端：小窗/悬浮按钮/快捷键/上下文捕获 - 形态：单 JS 文件 + Shadow DOM 隔离；最小接入 1-2 行 script - 交互：问答小窗 + “带我去”按钮 + 页面高亮定位 + 失败提示
· 采集与索引：站点抓取、可交互元素抽取、文本清洗 - 采集输出：pageUrl、pageTitle、elementText、selector/xpath、contextText、nearestHeading - 增量策略：页面指纹/校验和；变更页优先重抓
· 知识库与检索：向量索引 + 结构化索引 - 结构化优先：ActionableElement 优先召回；向量补足语义
· 问答与路由：答案生成 + 页面/功能定位 + 证据展示 - 输出带结构化定位数据（pageUrl + selector + evidence）
· 运维后台：站点配置、采集任务、索引状态、诊断 - P1 可选：最小后台或 API 管理

3.4 数据与接口规划
· 采集输出格式：页面 + 元素（最小字段即可）- 元素元数据：text、tag、selector/xpath、pageUrl、pageTitle、contextText、nearestHeading
· 索引实体：ActionableElement、Paragraph 两类即可覆盖 MVP
· API 形态（最小闭环）- POST /crawl/start：触发抓取（siteId、startUrl、allowedDomains）- GET /crawl/status：抓取状态与结果位置 - POST /kb/build-from-crawl：接收结构化抓取数据并建库 - POST /chat：返回 answer + locations[]（pageUrl、selector、evidence）- POST /locate（可选）：前端上报定位结果与失败原因

3.5 集成方式规划
· 方式 A（推荐）：前端 SDK 嵌入 + 后端服务托管 - 配置方式：window.**SITE_NAV_CONFIG** + async widget.js
· 方式 B（备选）：一体化轻部署（可选本地/私有化）- 部署最小化：爬虫服务 + RAG 后端 + 静态 Widget
· 权限策略：只读抓取 + 禁止危险操作默认策略 - 白名单与同源限制；登录态抓取需显式授权

3.6 里程碑与阶段目标
· P0（验证，2 周）：单站点最小闭环 demo - 端到端验证：问答 -> 页面定位 -> 高亮
· P1（可用，4 周）：可嵌入 + 可配置抓取 + 跨页定位 - 支持主题配置与基础鉴权
· P2（可扩展）：多站点、多租户、权限隔离、运维面板 - 增量爬取与变更检测自动重索引

4. 下一步详细规划（落地版）

4.1 MVP 功能清单
· Widget 原型：悬浮入口 + 问答小窗 + “带我去”按钮 - 支持最小接入脚本与 Shadow DOM 隔离
· 采集 -> 索引 -> 问答 -> 定位 的端到端链路 - 采集输出包含 selector/pageUrl/contextText
· 结果展示：答案 + 来源页面 + 可点击定位 - 返回结构化定位字段供前端执行

4.2 关键技术决策
· 采集方式：手动触发为主，P1 增加定时与增量更新 - DOM 指纹用于判断增量更新
· 索引策略：结构化索引 + 向量索引融合 - 结构化优先召回，向量用于语义补齐
· 模型策略：嵌入模型 + 生成模型可切换 - 生成侧输出 JSON 定位块

4.3 风险与约束
· 登录态与权限抓取边界 - 默认不携带 cookie；管理员显式配置后才可抓取
· DOM 变化导致定位失效 - 多级定位容错：ID/data-\* -> 选择器 -> 文本锚点
· 采集成本与抓取性能 - 并发限制、资源阻断、遵守 robots

4.4 验收与指标
· 定位准确率 - 页面内高亮成功率 >= 80%
· 问答可用率 - 可解释来源比例 >= 90%
· 集成时间与部署复杂度 - 最小接入 <= 15 分钟，脚本接入步骤 <= 2

4.5 执行排期（建议）
· 第 1-2 周：需求冻结 -> 采集增强 -> /kb/build-from-crawl -> Widget 原型
· 第 3-6 周：跨页定位 -> 主题配置 -> 基础鉴权 -> 试点验证
· 第 7 周起：多站点隔离与运维面板（按需求迭代）

---

## 5. 安全加固计划（落地优先）

> 结合当前代码实际，优先修复可立即落地的安全问题，不做无对应代码的假设性修补。

### 5.1 当前代码中存在的安全问题

| #   | 问题                                             | 所在模块                                                | 影响                                              | 优先级 |
| --- | ------------------------------------------------ | ------------------------------------------------------- | ------------------------------------------------- | ------ |
| 1   | CORS 允许任意 Origin                             | `DocVec_RAG/backend` — `application.yml` / `CorsConfig` | 任意域可调用 /api/\*\*，可被恶意页面窃取对话数据  | 🔴 P0  |
| 2   | `/api/chat` / `/api/knowledge-base/*` 无速率限制 | `DocVec_RAG/backend` — Controller 层                    | 可被刷接口消耗 embedding / LLM 配额               | 🟡 P1  |
| 3   | 爬虫敏感词过滤仅客户端侧                         | `Web_intelligent_function_navigation/main.js`           | 恶意构造的爬取任务可能触发危险按钮（删除/支付等） | 🟡 P1  |
| 4   | Widget 无 CSP / 无来源校验                       | `embedded-widget/widget.js`                             | 若被插入恶意页面，Widget 可被用作钓鱼前端         | 🟢 P2  |
| 5   | 后端错误信息可能暴露内部细节                     | `DocVec_RAG/backend` — 全局异常处理                     | 生产环境泄露堆栈/路径                             | 🟢 P2  |

### 5.2 修复方案与落地步骤

#### 🔴 P0-1：CORS Origin 白名单

**位置**: `DocVec_RAG/backend/src/main/resources/application.yml`

**改动**:

```yaml
# 新增 CORS 配置项
app:
  cors:
    allowed-origins:
      - http://localhost:3456 # 爬虫控制台
      - http://localhost:4567 # demo 站点
      - http://localhost:5173 # 前端开发服务器
```

**后端 Java 侧（可选增强）**: 如果已有 `CorsConfig.java`，改为读取 `app.cors.allowed-origins`；如果用的是 `application.yml` 的 `spring.web.cors`，则在 yml 中直接配置。

**验证**: `curl -H "Origin: http://evil.com" http://localhost:8080/api/chat` 应返回无 `Access-Control-Allow-Origin` 头（blocked）。

---

#### 🟡 P1-1：/api/chat 速率限制

**位置**: `DocVec_RAG/backend`

**方案**: 引入 Spring Boot 拦截器或 Bucket4j，对 `/api/chat` 和 `/api/knowledge-base/build-from-crawl` 按 IP + kbName 做速率控制。

**落地策略**（最小成本）:

1. 新增 `RateLimitInterceptor.java`，实现 `HandlerInterceptor.preHandle()`
2. 用 `ConcurrentHashMap<String, long[]>` 做内存级滑动窗口（60s 内最多 10 次 chat）
3. 超限返回 `429 Too Many Requests`

**阈值**:

- `/api/chat`: 10 req/min per IP
- `/api/knowledge-base/build-from-crawl`: 5 req/min per IP

---

#### 🟡 P1-2：敏感词过滤服务端校验

**位置**: `Web_intelligent_function_navigation/server.js` — `/api/crawl/start`

**改动**: 在 `server.js` 的 crawl/start handler 中，校验传入的 `sensitiveWords` 是否被篡改（确保默认黑名单至少包含危险词）。不允许通过 API 参数完全覆盖敏感词列表——只能追加，不能移除默认项。

```javascript
const DEFAULT_SENSITIVE_WORDS =
  "delete,remove,pay,submit,confirm,logout,reset,clear,删除,注销,支付,清空,重置";
const userSensitiveWords = config.sensitiveWords || "";
const mergedSensitiveWords = [
  ...new Set([
    ...DEFAULT_SENSITIVE_WORDS.split(",")
      .map((s) => s.trim())
      .filter(Boolean),
    ...userSensitiveWords
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean),
  ]),
].join(",");
```

---

#### 🟢 P2-1：Widget 来源校验

**位置**: `embedded-widget/widget.js`

**改动**: Widget 初始化时校验 `window.location.origin` 是否在允许列表中（从 `__SITE_NAV_CONFIG__.allowedOrigins` 读取）。不在白名单内则拒绝初始化，console.warn 提示。

```javascript
const allowedOrigins = config.allowedOrigins || [window.location.origin];
if (!allowedOrigins.includes(window.location.origin)) {
  console.warn("[SiteNav] Unauthorized origin:", window.location.origin);
  return;
}
```

---

#### 🟢 P2-2：全局异常处理不泄露内部细节

**位置**: `DocVec_RAG/backend` — `GlobalExceptionHandler.java`（若无则新建）

**改动**: 所有未处理异常统一返回 `{"error":"Internal server error"}`，不携带 stacktrace。仅在 `spring.profiles.active=dev` 时返回详细错误。

### 5.3 安全修复排期

| 阶段       | 内容                                        | 预计耗时 |
| ---------- | ------------------------------------------- | -------- |
| **当天**   | CORS 白名单配置 + 验证                      | 30 min   |
| **本周**   | `/api/chat` 速率限制 + 爬虫敏感词服务端校验 | 2 h      |
| **本迭代** | Widget 来源校验 + 异常处理不泄露            | 1.5 h    |

> 修复后均需回归：crawl → build → chat → widget locate 端到端链路不受影响。
