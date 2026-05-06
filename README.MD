# FuncFind — 智能功能导航系统

基于 **RAG（检索增强生成）** 的网页功能定位与导航平台。自动爬取网站、提取可交互元素、构建知识库，并通过嵌入式 Widget 实现自然语言驱动的页面功能查找与跨页导航。

---

## 项目概述

| 维度        | 说明                                                       |
| ----------- | ---------------------------------------------------------- |
| 项目类型    | 智能网页功能导航 + RAG 知识检索                            |
| 核心能力    | 网站爬取 → 元素提取 → 知识库构建 → 自然语言定位 → 跨页导航 |
| 后端        | Java Spring Boot（Maven）                                  |
| 爬虫        | Node.js + Crawlee + Playwright                             |
| 前端 Widget | 原生 JS（Shadow DOM，零依赖嵌入）                          |
| 向量库基线  | Cloudflare Vectorize（当前占位实现）                       |
| 当前阶段    | 核心链路可跑通，生产级 Cloudflare 接入进行中               |

---

## 系统架构

```
┌────────────────────────────────────────────────────────────────┐
│                     FuncFind 系统                               │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐    ┌──────────────────────────────┐      │
│  │  爬虫控制台 (3456) │───▶│  RAG 后端 (8080)              │      │
│  │  Crawlee+Playwright│   │  Spring Boot                  │      │
│  │  · 网站爬取        │   │  · 文档/元素上传               │      │
│  │  · 交互元素提取    │   │  · 知识库构建                  │      │
│  │  · XPath 生成      │   │  · 向量检索                    │      │
│  │  · 增量 checksum   │   │  · Chat 问答                   │      │
│  └────────┬───────────┘   │  · 定位事件日志                │      │
│           │               └──────────────┬─────────────────┘      │
│           │  /api/rag/                   │                        │
│           │  build-from-crawl            │ /api/chat              │
│           │                              │ /api/locate            │
│           ▼                              ▼                        │
│  ┌──────────────────┐    ┌──────────────────────────────┐      │
│  │  嵌入式 Widget    │◀───│  目标网站 (site-demo)         │      │
│  │  Shadow DOM       │   │  · 零侵入集成                 │      │
│  │  · 自然语言提问   │   │  · 跨页导航                   │      │
│  │  · XPath 定位     │   │  · 元素高亮                   │      │
│  │  · 降级回退       │   │                              │      │
│  └──────────────────┘    └──────────────────────────────┘      │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### 核心数据流

```mermaid
flowchart LR
    A[目标网站] -->|爬取| B[爬虫引擎]
    B -->|sitemap.json| C[爬虫控制台]
    C -->|POST /api/rag/build-from-crawl| D[RAG 后端]
    D -->|Chunk + Embedding| E[向量库]
    F[用户提问] -->|Widget| D
    D -->|Top-K 检索| E
    E -->|上下文块| D
    D -->|答案 + 定位提示| F
    F -->|XPath 定位| G[目标页面元素]
    G -.->|失败回退| H[选择器/文本降级]
    H -.->|日志| I[/api/locate]
```

---

## 仓库结构

```
FuncFind/
├── DocVec_RAG/                          # RAG 后端 + 前端
│   ├── backend/                         # Java Spring Boot 后端
│   │   ├── src/main/java/com/docvecrag/backend/
│   │   │   ├── controller/              # REST API 控制器
│   │   │   ├── service/                 # 业务逻辑层
│   │   │   │   ├── ChatService.java     # 问答 + 定位提示
│   │   │   │   ├── CrawlKnowledgeBaseBuildService.java
│   │   │   │   └── locate/              # 定位事件日志
│   │   │   └── dto/                     # 数据传输对象
│   │   ├── API_CONTRACT.md              # API 契约（已冻结）
│   │   └── pom.xml
│   ├── frontend/                        # Vue 3 前端
│   │   ├── index.html
│   │   ├── app.js
│   │   └── styles.css
│   ├── Prompt                           # 产品与架构目标
│   ├── ARCHITECTURE.md                  # 架构决策记录
│   ├── AGENTS.md                        # 开发规范
│   └── DEVLOG.md                        # 开发日志
│
├── Web_intelligent_function_navigation/ # 爬虫引擎 + 控制台
│   ├── main.js                          # Crawlee 爬虫主程序
│   ├── server.js                        # Express + Socket.IO 控制台服务
│   ├── package.json
│   ├── public/
│   │   ├── index.html                   # 爬虫控制台 UI
│   │   └── crawl-report.html            # 结果报告页
│   ├── database/                        # 爬取结果存储
│   ├── outputs/                         # 爬取输出
│   ├── README.md                        # 爬虫使用文档
│   └── User.md                          # 非技术用户指南
│
├── embedded-widget/                     # 嵌入式定位 Widget
│   ├── widget.js                        # Shadow DOM Widget 核心
│   ├── demo.html                        # 独立演示页
│   └── README.md                        # 嵌入使用说明
│
├── site-demo/                           # 测试用演示站点
│   ├── server.js                        # 静态文件服务
│   └── public/
│       ├── index.html                   # 首页
│       └── reports.html                 # 报表页（跨页测试）
│
├── plan.md                              # 产品规划 + 安全加固计划
├── README.MD                            # 本文件
└── Complete_the_record.md               # 任务完成日志
```

---

## 文件依赖关系图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          FuncFind 文件依赖                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  控制台页面 (public/index.html)                                         │
│  ├── 调用 → /api/crawl/start ──▶ server.js ──▶ main.js (爬虫)           │
│  │                                      │                               │
│  │                                      └──▶ database/*.json (输出)      │
│  ├── 调用 → /api/rag/build-from-crawl ──▶ server.js                     │
│  │           │                         ──▶ RAG backend (8080)            │
│  │           └── 成功后 ──▶ localStorage.crawledKbList (浏览器)          │
│  │                      ──▶ server.js builtKbs[] (内存)                  │
│  └── 导航 → /rag/ ──▶ DocVec_RAG/frontend/ (静态服务)                    │
│                                                                         │
│  RAG 前端 (DocVec_RAG/frontend/)                                        │
│  ├── index.html ←→ app.js ←─ styles.css                                 │
│  ├── created() ──▶ fetch /api/rag/kbs ──▶ server.js builtKbs[]          │
│  ├── created() ──▶ localStorage.crawledKbList ──▶ 跨页面共享             │
│  ├── useCrawledKb() ──▶ settings.kbName = "demo-kb"                     │
│  │                   ──▶ kbReady = true                                 │
│  └── sendMessage() ──▶ fetch /api/chat ──▶ RAG backend (8080)           │
│                                                                         │
│  Widget (embedded-widget/widget.js)                                     │
│  ├── 配置来源 → window.__SITE_NAV_CONFIG__ (由宿主页面设置)               │
│  ├── ask ──▶ POST /api/chat ──▶ RAG backend (8080)                      │
│  ├── locateElement() ──▶ XPath 优先 → selector → text 降级               │
│  └── 失败 ──▶ POST /api/locate ──▶ RAG backend logs/                    │
│                                                                         │
│  Demo 站点 (site-demo/)                                                 │
│  ├── server.js ──▶ public/index.html + reports.html                     │
│  ├── 引用 → embedded-widget/widget.js                                   │
│  └── 设置 → __SITE_NAV_CONFIG__ = { apiBase, kbName, ... }              │
│                                                                         │
│  爬虫 (main.js)                                                         │
│  ├── 读取基线 → --baseline (增量对比用)                                   │
│  ├── hashContent() ──▶ checksum 写入输出                                  │
│  └── 输出 → --out 参数 / OUTPUT_PATH / DATABASE_PATH                     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 数据存放位置

| 数据类型           | 位置                                            | 持久化              |
| ------------------ | ----------------------------------------------- | ------------------- |
| 爬取结果 JSON      | `Web_intelligent_function_navigation/database/` | ✅ 磁盘文件         |
| 旧爬取数据（兼容） | `Web_intelligent_function_navigation/outputs/`  | ✅ 磁盘文件（只读） |
| KB 服务端追踪      | `server.js` 内存 `builtKbs[]`                   | ❌ 重启丢失         |
| KB 浏览器端追踪    | `localStorage.crawledKbList`                    | ✅ 浏览器持久化     |
| 向量索引           | Cloudflare Vectorize（通过 RAG backend）        | ✅ Cloudflare       |
| 定位失败日志       | `DocVec_RAG/backend/logs/locate-events.jsonl`   | ✅ 磁盘文件         |

> **爬取输出已统一到 `database/`**，爬虫控制台的"数据库爬取索引"面板可直接看到新结果。`outputs/` 保留用于兼容旧数据。

````

---

## 快速开始

### 环境要求

| 组件    | 要求                            |
| ------- | ------------------------------- |
| Java    | JDK 17+                         |
| Maven   | 3.8+（或使用 `mvnw`）           |
| Node.js | 18+                             |
| npm     | 9+                              |
| 浏览器  | Chromium（Playwright 自动安装） |

### 1. 启动 RAG 后端

```powershell
cd DocVec_RAG\backend
.\mvnw.cmd spring-boot:run
````

后端默认运行在 `http://localhost:8080`。

### 2. 启动爬虫控制台

```powershell
cd Web_intelligent_function_navigation
npm install
npx playwright install chromium
node server.js
```

控制台运行在 `http://localhost:3456`。

### 3. 运行一次爬取

在浏览器打开 `http://localhost:3456`，输入目标网址并点击"开始爬取"。或通过 API：

```powershell
$body = @{ url = "https://example.com"; maxRequests = 10; maxConcurrency = 1 } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:3456/api/crawl/start -Method Post -ContentType 'application/json' -Body $body
```

### 4. 将爬取结果推送到 RAG

```powershell
$body = @{ ragBaseUrl = "http://localhost:8080"; kbName = "default-kb"; dataset = "latest" } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:3456/api/rag/build-from-crawl -Method Post -ContentType 'application/json' -Body $body
```

### 5. 测试问答

```powershell
$body = @{ question = "export report"; kbName = "default-kb"; topK = 3 } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8080/api/chat -Method Post -ContentType 'application/json' -Body $body
```

### 6. 嵌入 Widget

在目标网页中添加：

```html
<script>
  window.__SITE_NAV_CONFIG__ = {
    apiBase: "http://localhost:8080",
    kbName: "default-kb",
    topK: 6,
    title: "Feature Finder",
    subtitle: "Ask where a feature lives",
  };
</script>
<script src="path/to/widget.js"></script>
```

---

## API 接口速览

### RAG 后端 (`localhost:8080`)

| 端点                                   | 方法 | 说明                 |
| -------------------------------------- | ---- | -------------------- |
| `/api/documents/upload`                | POST | 上传文档             |
| `/api/knowledge-base/build`            | POST | 从文档构建知识库     |
| `/api/knowledge-base/build-from-crawl` | POST | 从爬取结果构建知识库 |
| `/api/chat`                            | POST | 问答（返回定位提示） |
| `/api/locate`                          | POST | 上报定位失败事件     |
| `/api/runtime-config/apply`            | POST | 应用运行时配置       |
| `/api/runtime-config/current`          | GET  | 查看当前配置         |
| `/api/fallback-events`                 | GET  | 查询回退事件         |

### 爬虫控制台 (`localhost:3456`)

| 端点                          | 方法 | 说明             |
| ----------------------------- | ---- | ---------------- |
| `/api/crawl/start`            | POST | 启动爬取         |
| `/api/crawl/stop`             | POST | 停止爬取         |
| `/api/status`                 | GET  | 爬取状态         |
| `/api/results`                | GET  | 获取结果         |
| `/api/rag/build-from-crawl`   | POST | 推送到 RAG       |
| `/api/rag/build-from-element` | POST | 单元素推送到 RAG |
| `/api/datasets`               | GET  | 列出历史数据集   |
| `/api/results/download`       | GET  | 下载 JSON 结果   |

完整 API 文档见 [DocVec_RAG/backend/API_CONTRACT.md](DocVec_RAG/backend/API_CONTRACT.md)。

---

## 核心特性

### 🕷 智能网站爬取

- 基于 Crawlee + Playwright，支持 Chromium / Edge
- 自动提取可交互元素（链接、按钮、提交按钮等）
- 生成结构化输出：`selector`、`xpath`、`elementText`、`tag`、`context` 等
- 支持增量抓取（checksum 对比，跳过未变化页面）
- 可配置阻挡资源类型、危险词过滤、并发数

### 🔍 知识库构建与检索

- 两段式分块：固定长度粗切 + 语义策略收口
- embedding 索引与查询侧强制同模型同维度
- 支持文档上传（PDF/DOCX/TXT）与爬取数据两种入库路径
- 检索结果附带完整定位元数据（XPath、选择器、页面 URL）

### 💬 嵌入式 Widget

- Shadow DOM 隔离，零样式冲突
- 自然语言输入，自动检索并返回定位建议
- XPath 优先定位，选择器/文本降级回退
- 跨页导航：目标元素在不同页面时自动导航
- 定位失败自动上报 `/api/locate`

### 📊 爬虫控制台

- Web UI：配置爬取参数 → 实时日志 → 结果浏览
- 报告页：页面列表 + 元素详情 + 筛选搜索
- 单元素推送与验证（Push/Verify 按钮）
- 数据库历史管理

---

## 定位策略

Widget 定位元素的优先级：

1. **XPath** — 最精确，从 `locations[].xpath` 获取
2. **CSS 选择器** — 回退方案，从 `locations[].selector` 获取
3. **元素文本** — 最终降级，遍历页面匹配 `elementText`

定位失败时自动向 `/api/locate` 上报事件（JSONL 格式），包含页面 URL、尝试的选择器/XPath、失败原因和时间戳。

---

## 增量爬取

通过 `--baseline` 参数指定基线数据集，爬虫会对比每个页面的 checksum：

- 页面内容未变化 → 复用基线元素数据，跳过重新提取
- 页面内容变化 → 重新提取交互元素

```bash
node main.js --url https://example.com --baseline database/sitemap.json
```

---

## 技术栈

| 层级           | 技术                          |
| -------------- | ----------------------------- |
| 后端框架       | Spring Boot 3.x               |
| 构建工具       | Maven（mvnw）                 |
| 爬虫引擎       | Crawlee + Playwright          |
| 控制台服务     | Express + Socket.IO           |
| Widget         | 原生 JavaScript（Shadow DOM） |
| 向量库（基线） | Cloudflare Vectorize          |
| 嵌入模型       | Workers AI / all-MiniLM-L6-v2 |
| 生成模型       | Workers AI / gpt-4o-mini      |
| 对象存储       | Cloudflare R2（占位）         |

---

## 开发指南

- 需求主线以 [DocVec_RAG/Prompt](DocVec_RAG/Prompt) 为准
- 架构决策见 [DocVec_RAG/ARCHITECTURE.md](DocVec_RAG/ARCHITECTURE.md)
- 开发规范见 [DocVec_RAG/AGENTS.md](DocVec_RAG/AGENTS.md)
- API 契约见 [DocVec_RAG/backend/API_CONTRACT.md](DocVec_RAG/backend/API_CONTRACT.md)
- 操作日志见 [Complete_the_record.md](Complete_the_record.md)

### 常用命令

```powershell
# 后端构建与测试
cd DocVec_RAG\backend
.\mvnw.cmd clean package
.\mvnw.cmd test

# 爬虫安装
cd Web_intelligent_function_navigation
npm install
npx playwright install chromium

# 演示站点
cd site-demo
node server.js
```

---

## 路线图

- [x] 核心链路：爬取 → 知识库构建 → Chat → 定位
- [x] XPath 提取与优先定位
- [x] 嵌入式 Widget（Shadow DOM）
- [x] 定位失败追踪（`/api/locate`）
- [x] 单元素推送与验证 UI
- [x] 增量爬取（checksum）
- [ ] Cloudflare Vectorize 真实 API 接入
- [ ] Cloudflare R2 存储替换
- [ ] Workers AI 生产模型切换
- [ ] Docker 容器化部署
- [ ] CI/CD 流水线

---

## 许可证

Proprietary — 内部开发项目。
