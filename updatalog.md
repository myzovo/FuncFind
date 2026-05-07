# Update Log

## 2026-05-07 — 成熟度打磨（工程性优化）

本次改动以增量方式提升项目从"能工作"到"可靠、可维护、可交付"的成熟度，未做大规模重构。

---

### P0 - 安全性修复

#### 1. crawler/public/index.html — 修复 7+ 处 XSS 注入

- 新增 `escapeHtml()` 和 `sanitizeHref()` 辅助函数
- `loadDatasetIndex()`: innerHTML 拼接改为 DOM API + textContent
- `renderBarChart()`: innerHTML 拼接改为 DOM API + textContent
- `renderPagesTable()`: innerHTML 拼接改为 DOM API + textContent
- `renderDetail()` 卡片头和元素详情: innerHTML 拼接改为 DOM API + textContent
- `item.href` 注入 `href` 属性改为使用 `sanitizeHref()` 过滤 `javascript:` 协议

**原因**: 爬取的网页数据（标题、URL、元素文本等）被直接拼入 innerHTML，攻击者可在爬取目标页面中植入恶意脚本，通过报告页面执行 XSS。`item.href` 可注入 `javascript:alert(document.cookie)` 协议。

#### 2. backend/ApiExceptionHandler.java — 修复信息泄露

- `handleUnhandled` 不再返回 `ex.getMessage()` 给客户端，改为通用 "Internal server error"
- 所有 handler 添加日志记录（warn/error 级别）

**原因**: `ex.getMessage()` 可能包含堆栈细节、内部路径等敏感信息。无日志导致 500 错误在服务端无任何痕迹。

#### 3. backend/DocumentController.java — 添加文件类型验证

- 新增 `.txt, .md, .csv, .json, .log` 扩展名白名单
- `validateFile()` 方法检查文件名和扩展名

**原因**: 原实现无任何文件类型校验，可上传任意文件（.exe, .sh 等）。

#### 4. backend/DocumentIngestionService.java — 移除静默占位符

- 非支持文件类型直接抛出 `IllegalArgumentException` 而非返回 `[extracted-placeholder]`
- `safeFilename` 使用 `Paths.get().getFileName()` 防止路径遍历
- 添加关键路径日志

**原因**: 原实现对 PDF/DOCX 等文件静默返回占位符字符串，生成无意义的向量数据，用户无任何提示。

---

### P1 - 错误处理修复

#### 5. frontend/app.js — 修复错误处理

- `uploadDocuments`: 检查 `response.ok`，失败时抛出具体错误
- `buildKnowledgeBase`: 检查 `response.ok`，失败时设 `kbReady=false` 并显示错误
- 外层 try 添加 catch 块，防止错误未处理
- `useCrawledKb`: 后端不可用时不再静默设 `kbReady=true`
- `clearCrawledKbs`: fetch 失败添加 console.warn
- 添加 `beforeUnmount` 生命周期钩子清理事件监听

**原因**: 原实现多处空 catch 块静默吞掉错误，fetch 不检查 response.ok，buildKnowledgeBase 失败仍显示"构建完成"。事件监听未清理可能导致内存泄漏。

#### 6. backend 核心 Service 添加日志

- `ChatService`: 请求进入、检索结果、异常情况
- `RetrievalService`: 检索开始和结果（debug 级别）
- `KnowledgeBaseBuildService`: 构建开始、文档数、索引完成
- `CrawlKnowledgeBaseBuildService`: 构建开始、索引完成
- `DocumentIngestionService`: 文档摄入开始和完成

**原因**: 原实现绝大多数 Service 和所有 Controller 完全没有日志，生产环境无法追踪请求链路和排查问题。

---

### P2 - 配置收拢

#### 7. 新建 backend/config/Defaults.java — 统一默认值

```java
public static final String GENERATION_MODEL = "gpt-4o-mini";
public static final String EMBEDDING_MODEL = "all-MiniLM-L6-v2";
public static final String KB_NAME = "default-kb";
public static final int TOP_K = 6;
public static final int CHUNK_SIZE = 500;
public static final double SEMANTIC_THRESHOLD = 0.78;
```

更新了以下文件引用 Defaults:
- ChatRequest, KnowledgeBaseBuildRequest, CrawlBuildRequest
- ChatService, DocumentIngestionService, SessionRuntimeConfigService
- ExternalModelServiceAdapter, InMemoryRawTextStore, R2FallbackRawTextStore
- InMemoryR2RawTextClient, CloudflareVectorizeClientStub

**原因**: 同一个默认值 `"gpt-4o-mini"` 原先分散在 4 个不同文件中，更改默认模型名需要在多处同步修改，极易遗漏。

#### 8. frontend/app.js — predefinedModels 收拢

- 提取 `PREDEFINED_MODELS` 为模块级常量
- 两处内联定义改为引用常量

**原因**: `predefinedModels` 数组在两处完全重复定义，新增模型需要编辑两个位置。

---

### P3 - 代码质量修复

#### 9. demo/server.js — MIME 类型和错误处理

- 新增 `MIME_TYPES` 映射对象（支持 .html, .css, .js, .json, .png, .jpg, .svg, .ico, .woff, .woff2）
- `sendFile` 改用 stream error 事件监听替代 `fs.existsSync` 检查（修复 TOCTOU 竞态）

**原因**: 原实现只支持 .html 和兜底的 text/plain，.css/.js 文件会以纯文本提供，浏览器拒绝执行。`existsSync` + `createReadStream` 存在竞态条件。

#### 10. widget/widget.js — 重复加载防护

- IIFE 顶部添加 `if (document.getElementById("site-nav-widget-host")) return;` 防止重复初始化

**原因**: 如果页面意外包含两次 widget.js，会产生两个 host div 和两套 Shadow DOM。

---

### 未修改的已知问题（需要业务决策）

| 问题 | 说明 |
|------|------|
| backend CORS 全开 | `allowedOriginPatterns("*")` 需要确定允许的域名 |
| backend 无认证机制 | 需要选择认证方案（API Key / JWT / OAuth） |
| backend 存储全为内存实现 | 需要接入持久化存储（R2/Vectorize 的 Stub 实现） |
| crawler/server.js SSRF 风险 | RAG 端点接受任意 URL，需要 URL 白名单或内网过滤 |
| crawler/server.js CORS 全开 | 同上需要确定域名 |
| 两个 ModelAdapter 重复代码 | `preprocess` / `buildSystemPrompt` 几乎相同，可提取基类 |
| crawler/index.html 与 crawl-report.html 重复代码 | 报告渲染逻辑重复实现，建议统一 |
