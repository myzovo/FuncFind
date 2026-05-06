## Task Completion Record

Date: 2026-05-06

### Scope

本轮（基于上次 2026-05-05 记录继续）：

1. ✅ **增量爬取 checksum 支持** — 避免重复抓取未变更页面
2. ✅ **Demo 站点 + 新鲜全量爬取** — site-demo/ 含首页 + 报告页
3. ✅ **推送到 RAG 并验证 XPath** — chat 返回非空 xpath 字段
4. ✅ **Widget 跨页定位测试** — 在 demo 站点验证定位与降级链路
5. ✅ **爬虫控制台 ↔ RAG 前端打通** — 控制台新增"🤖 RAG 对话"按钮，/rag/ 路由服务 RAG 前端
6. ✅ **安全加固计划写入 plan.md** — 第 5 节，5 个问题 + 修复方案 + 排期
7. ✅ **数据目录统一到 database/** — 爬取输出从 outputs/ 改为 database/
8. ✅ **RAG 前端支持爬取知识库** — "📦 已爬取知识库"区域，无需上传文档即可聊天

### Changes Made (本轮)

#### main.js — 增量爬取 checksum

- 新增 `hashContent()` (sha256)、`loadBaselineData()`、`buildBaselineIndex()`
- CLI 参数 `--baseline` / 环境变量 `BASELINE_PATH`
- 爬取时对比 checksum，不变则复用 baseline 数据

#### site-demo/ — Demo 站点

- `server.js`：简单 HTTP server，端口 4567
- `public/index.html`：首页，含 widget + 跳转链接
- `public/reports.html`：报告页，含 Export Report / Download Summary 按钮

#### server.js — 多项改造

- 新增 `/rag/` 静态路由服务 DocVec_RAG 前端
- 爬取输出从 `outputsDir` 改为 `databaseDir` (统一数据目录)
- `resolveCrawlData()` 增加 outputs/ 回退兼容
- 新增 `GET /api/rag/kbs` 端点，返回服务端已构建 KB 列表
- `POST /api/rag/build-from-crawl` 响应增强：返回 kbName, dataset, pageCount, elementCount
- 服务端 `builtKbs[]` 追踪已推送的知识库

#### public/index.html — 多项改造

- hero-actions 新增 `🤖 RAG 对话` 按钮 → 导航到 /rag/
- `pushToRag()` 成功后写入 `localStorage.crawledKbList`

#### DocVec_RAG/frontend/ — RAG 前端改造

- `app.js`：新增 `crawledKbs`、`crawledKbActive` data；`loadCrawledKbs()` 从服务器 API + localStorage 加载；`useCrawledKb()` 一键激活爬取 KB；`clearCrawledKbs()` 清除
- `index.html`：新增 "📦 已爬取知识库" section（upload 与 chat 之间）
- `styles.css`：新增 `.crawl-kb-panel`、`.crawl-kb-item` 样式

#### plan.md — 安全加固计划

- 新增第 5 节，优先级 P0–P2
- CORS 白名单、速率限制、敏感词服务端校验、Widget 来源校验、异常处理

#### README.md (Web_intelligent_function_navigation) — 基线文档

- 新增 `--baseline` / `BASELINE_PATH` 说明

#### README.MD (根目录) — 系统文档

- 新增 "文件依赖关系图" 章节
- 新增 "数据存放位置" 表格

### Smoke Test — 2026-05-06

| Step                 | Command                                                                          | Result                             |
| -------------------- | -------------------------------------------------------------------------------- | ---------------------------------- |
| 1. 启动 demo 站点    | `cd site-demo; node server.js`                                                   | ✅ localhost:4567                  |
| 2. 启动后端          | `cd DocVec_RAG/backend; .\mvnw.cmd spring-boot:run`                              | ✅ localhost:8080                  |
| 3. 启动控制台        | `cd Web_intelligent_function_navigation; node server.js`                         | ✅ localhost:3456                  |
| 4. 新鲜爬取          | `POST /api/crawl/start {url:"http://localhost:4567/index.html", maxRequests:10}` | ✅ 2 pages, 4 elements, xpath 非空 |
| 5. 推送到 RAG        | `POST /api/rag/build-from-crawl {ragBaseUrl, kbName:"demo-kb"}`                  | ✅ 2 docs, 4 chunks indexed        |
| 6. Chat 查询         | `POST /api/chat {question:"export report", kbName:"demo-kb", topK:3}`            | ✅ 3 contextChunks + 3 locations   |
| 7. XPath 验证        | locations[0].xpath = "/body/div/a"                                               | ✅ 非空                            |
| 8. Widget 问答       | 首页点击"Open navigator" → 输入 "export report" → Ask                            | ✅ 返回 4 个定位结果               |
| 9. 跨页定位          | /rag/ 路由可访问 DocVec Studio                                                   | ✅ 页面完整                        |
| 10. 控制台 RAG 按钮  | 点击 "🤖 RAG 对话"                                                               | ✅ 跳转 /rag/                      |
| 11. /api/rag/kbs     | `GET /api/rag/kbs`                                                               | ✅ 返回 [{kbName:"demo-kb",...}]   |
| 12. RAG 前端爬取 KB  | /rag/ 显示 "📦 已爬取知识库"                                                     | ✅ demo-kb 显示在列表中            |
| 13. 激活爬取 KB      | 点击 "使用此知识库"                                                              | ✅ kbName→demo-kb, kbReady=true    |
| 14. 通过爬取 KB 聊天 | 输入 "export report" → 发送                                                      | ✅ AI 返回检索增强答案             |
| 15. 数据统一存储     | crawl → database/sitemap-\*.json                                                 | ✅ 爬虫索引面板可见                |

### Key Verification

- **XPath 字段非空**: 新鲜爬取产出 `/body/div/a`, `//*[@id="open-reports"]` 等真实 XPath
- **Widget 定位链**: ask → 返回 4 个 Locate 按钮 → 点击 Locate 执行定位
- **跨页打通**: 控制台 (3456) → RAG 对话 (3456/rag/) → 后端 API (8080)
- **增量 checksum**: `demo-sitemap.json` 中每页均含 `checksum` 字段
- **爬取 KB 聊天**: RAG 前端无需上传文档，直接通过 "📦 已爬取知识库" 选择 KB 开始对话
- **数据目录统一**: 爬取输出 → `database/`，旧数据 `outputs/` 仍可兼容读取
- **KB 发现机制**: 双重来源 — 服务器 API (`/api/rag/kbs`) + 浏览器 localStorage (`crawledKbList`)

### Notes

- `window.open` 可能被浏览器拦截弹窗；可直接导航到 `http://localhost:3456/rag/`
- 控制台服务器重启后内存中的 crawlResults 会丢失，但 outputs/demo-sitemap.json 持久保留
- `/api/locate` 日志文件: `DocVec_RAG/backend/logs/locate-events.jsonl`（widget 定位失败时写入）
