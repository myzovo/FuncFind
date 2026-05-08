# FuncFind — 页问

> **Language / 语言**: [中文](#中文) | [English](#english)

---

## 中文

基于 **RAG（检索增强生成）** 的网页功能定位与导航平台。自动爬取网站、提取可交互元素、构建知识库，通过自然语言驱动页面功能查找与跨页导航。

---

### 项目概述

| 维度     | 说明                                                       |
| -------- | ---------------------------------------------------------- |
| 项目类型 | 智能网页功能导航 + RAG 知识检索                            |
| 核心能力 | 网站爬取 → 元素提取 → 知识库构建 → 自然语言定位 → 跨页导航 |
| 后端     | Java Spring Boot（Maven）                                  |
| 爬虫     | Node.js + Crawlee + Playwright                             |
| 前端     | Vue 3（原生 JS，零构建工具依赖）                           |
| Widget   | 原生 JS（Shadow DOM，零依赖嵌入）                          |
| 向量库   | Cloudflare Vectorize（可替换）                             |
| 许可证   | MIT                                                        |

---

### 系统架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                        FuncFind 系统                                  │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────────────┐    ┌──────────────────────────────────┐        │
│  │  爬虫控制台      │───▶│  RAG 后端 (8080)                  │        │
│  │  Crawlee+Playwright│  │  Spring Boot                      │        │
│  │  · 网站爬取       │   │  · 文档/元素上传                   │        │
│  │  · 交互元素提取   │   │  · 知识库构建                      │        │
│  │  · XPath 生成     │   │  · 向量检索                        │        │
│  │  · 增量 checksum  │   │  · Chat 问答 + 定位提示            │        │
│  └────────┬──────────┘   └───────────────┬──────────────────┘        │
│           │                              │                            │
│           ▼                              ▼                            │
│  ┌─────────────────┐    ┌──────────────────────────────────┐        │
│  │  嵌入式 Widget   │◀───│  目标网站 (demo/)                 │        │
│  │  Shadow DOM      │    │  · 零侵入集成                     │        │
│  │  · 自然语言提问  │    │  · 跨页导航                       │        │
│  │  · XPath 定位    │    │  · 元素高亮                       │        │
│  │  · 降级回退      │    │                                   │        │
│  └─────────────────┘    └──────────────────────────────────┘        │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

---

### 目录结构

```
FuncFind/
├── backend/                # Java Spring Boot RAG 后端
│   ├── src/main/java/      # 源码（Controller / Service / DTO / Model）
│   ├── src/main/resources/ # 配置文件（application.yml）
│   ├── pom.xml             # Maven 依赖
│   └── mvnw                # Maven Wrapper
│
├── frontend/               # Vue 3 前端（零构建工具依赖）
│   ├── index.html          # 入口页面
│   ├── app.js              # Vue 应用逻辑
│   └── styles.css          # 全局样式
│
├── crawler/                # Node.js 爬虫引擎 + 控制台
│   ├── main.js             # Crawlee 爬虫主程序
│   ├── server.js           # Express + Socket.IO 控制台
│   ├── package.json
│   ├── public/             # 控制台 Web UI
│   ├── database/           # 爬取产物（sitemap.json）
│   └── outputs/            # 导出输出
│
├── widget/                 # 嵌入式定位 Widget
│   ├── widget.js           # 核心逻辑（Shadow DOM）
│   ├── demo.html           # 独立演示页面
│   └── README.md
│
├── demo/                   # 演示站点（Widget 集成示例）
│   ├── server.js           # 简单 HTTP Server (端口 4567)
│   └── public/
│
├── docs/                   # 文档
│   ├── ARCHITECTURE.md     # 架构决策记录
│   ├── API_CONTRACT.md     # API 契约文档
│   └── USER_GUIDE.md       # 用户操作指南
│
├── README.md               # 本文件
├── LICENSE                 # MIT 许可证
├── CONTRIBUTING.md         # 贡献指南
└── .gitignore
```

---

### 快速开始

#### 环境要求

| 组件   | 要求                            |
| ------ | ------------------------------- |
| 后端   | Java 17+, Maven 3.6+            |
| 爬虫   | Node.js 18+                     |
| 浏览器 | Chromium（Playwright 自动安装） |

#### 1. 启动 RAG 后端

```bash
cd backend
./mvnw spring-boot:run
# API 运行在 http://localhost:8080
```

#### 2. 启动爬虫控制台（入口）

```bash
cd crawler
npm install
npx playwright install chromium
node server.js
# 控制台运行在 http://localhost:3456
```

#### 3. 启动 Demo 站点（可选）

```bash
cd demo
node server.js
# Demo 运行在 http://localhost:4567
```

#### 4. 完整链路验证

1. 打开爬虫控制台 → 输入目标 URL → 点击"开始爬取"
2. 爬取完成后点击"推送到 RAG" → 自动构建知识库
3. 打开 Demo 站点 → 点击右下角 Widget → 用自然语言提问
4. Widget 返回定位结果 → 高亮目标元素 / 跳转页面

---

### Widget 嵌入指南

在任意网页中嵌入 FuncFind Widget，只需两行代码：

```html
<script>
  window.__SITE_NAV_CONFIG__ = {
    apiBase: "http://localhost:8080", // RAG 后端地址
    kbName: "default-kb", // 知识库名称
    topK: 6, // 检索数量
    title: "Site Navigator", // Widget 标题
    subtitle: "Ask where a feature lives",
  };
</script>
<script src="widget/widget.js"></script>
```

Widget 使用 Shadow DOM 实现样式隔离，不会影响宿主页面样式。

---

### API 概览

#### RAG 后端 (`localhost:8080`)

| 端点                                   | 方法 | 说明                 |
| -------------------------------------- | ---- | -------------------- |
| `/api/documents/upload`                | POST | 上传文档             |
| `/api/knowledge-base/build`            | POST | 从文档构建知识库     |
| `/api/knowledge-base/build-from-crawl` | POST | 从爬取结果构建知识库 |
| `/api/chat`                            | POST | 问答（返回定位提示） |
| `/api/locate`                          | POST | 上报定位失败事件     |
| `/api/runtime-config/apply`            | POST | 应用运行时配置       |
| `/api/runtime-config/current`          | GET  | 查看当前配置         |

#### 爬虫控制台 (`localhost:3456`)

| 端点                        | 方法 | 说明           |
| --------------------------- | ---- | -------------- |
| `/api/crawl/start`          | POST | 启动爬取       |
| `/api/crawl/stop`           | POST | 停止爬取       |
| `/api/status`               | GET  | 爬取状态       |
| `/api/results`              | GET  | 获取结果       |
| `/api/rag/build-from-crawl` | POST | 推送到 RAG     |
| `/api/datasets`             | GET  | 列出历史数据集 |

完整 API 文档见 [docs/API_CONTRACT.md](docs/API_CONTRACT.md)。

---

### 技术栈

- **后端**: Spring Boot 3.3, Java 17, Maven
- **向量**: Cloudflare Vectorize（可替换为 Pinecone / Weaviate / Milvus）
- **Embedding**: all-MiniLM-L6-v2 (384 维)
- **爬虫**: Crawlee 3.x + Playwright 1.44
- **前端**: Vue 3 (ES Module, 无构建步骤)
- **Widget**: 原生 JS, Shadow DOM, 零依赖
- **通信**: REST API, Server-Sent Events (SSE)

---

### 定位策略

Widget 定位元素的优先级：

1. **XPath** — 最精确，从 `locations[].xpath` 获取
2. **CSS 选择器** — 回退方案，从 `locations[].selector` 获取
3. **元素文本** — 最终降级，遍历页面匹配 `elementText`

定位失败时自动向 `/api/locate` 上报事件（JSONL 格式），包含页面 URL、尝试的选择器/XPath、失败原因和时间戳。

---

### 文档索引

- [架构决策记录](docs/ARCHITECTURE.md)
- [API 契约](docs/API_CONTRACT.md)
- [用户操作指南](docs/USER_GUIDE.md)
- [贡献指南](CONTRIBUTING.md)

---

## English

A **RAG (Retrieval-Augmented Generation)** based web feature location and navigation platform. Automatically crawls websites, extracts interactive elements, builds knowledge bases, and enables natural-language-driven page feature discovery and cross-page navigation.

---

### Project Overview

| Dimension     | Description                                                                      |
| ------------- | -------------------------------------------------------------------------------- |
| Project Type  | Intelligent Web Feature Navigation + RAG Knowledge Retrieval                     |
| Core Capability | Website Crawling → Element Extraction → Knowledge Base → NL Location → Cross-page Navigation |
| Backend       | Java Spring Boot (Maven)                                                         |
| Crawler       | Node.js + Crawlee + Playwright                                                   |
| Frontend      | Vue 3 (Vanilla JS, zero build-tool dependency)                                   |
| Widget        | Vanilla JS (Shadow DOM, zero-dependency embedding)                               |
| Vector Store  | Cloudflare Vectorize (swappable)                                                 |
| License       | MIT                                                                              |

---

### System Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                        FuncFind System                                │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────────────┐    ┌──────────────────────────────────┐        │
│  │  Crawler Console │───▶│  RAG Backend (8080)               │        │
│  │  Crawlee+Playwright│  │  Spring Boot                      │        │
│  │  · Website Crawl │   │  · Document/Element Upload         │        │
│  │  · Element Extract│   │  · Knowledge Base Build            │        │
│  │  · XPath Gen     │   │  · Vector Retrieval                │        │
│  │  · Incremental   │   │  · Chat Q&A + Location Hints       │        │
│  └────────┬──────────┘   └───────────────┬──────────────────┘        │
│           │                              │                            │
│           ▼                              ▼                            │
│  ┌─────────────────┐    ┌──────────────────────────────────┐        │
│  │  Embedded Widget │◀───│  Target Website (demo/)           │        │
│  │  Shadow DOM      │    │  · Zero-intrusion Integration     │        │
│  │  · NL Question   │    │  · Cross-page Navigation          │        │
│  │  · XPath Locate  │    │  · Element Highlight              │        │
│  │  · Fallback      │    │                                   │        │
│  └─────────────────┘    └──────────────────────────────────┘        │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

---

### Directory Structure

```
FuncFind/
├── backend/                # Java Spring Boot RAG backend
│   ├── src/main/java/      # Source (Controller / Service / DTO / Model)
│   ├── src/main/resources/ # Config (application.yml)
│   ├── pom.xml             # Maven dependencies
│   └── mvnw                # Maven Wrapper
│
├── frontend/               # Vue 3 frontend (zero build-tool dependency)
│   ├── index.html          # Entry page
│   ├── app.js              # Vue application logic
│   └── styles.css          # Global styles
│
├── crawler/                # Node.js crawler engine + console
│   ├── main.js             # Crawlee crawler main program
│   ├── server.js           # Express + Socket.IO console
│   ├── package.json
│   ├── public/             # Console Web UI
│   ├── database/           # Crawl artifacts (sitemap.json)
│   └── outputs/            # Export outputs
│
├── widget/                 # Embedded location widget
│   ├── widget.js           # Core logic (Shadow DOM)
│   ├── demo.html           # Standalone demo page
│   └── README.md
│
├── demo/                   # Demo site (Widget integration example)
│   ├── server.js           # Simple HTTP Server (port 4567)
│   └── public/
│
├── docs/                   # Documentation
│   ├── ARCHITECTURE.md     # Architecture decision records
│   ├── API_CONTRACT.md     # API contract documentation
│   └── USER_GUIDE.md       # User operation guide
│
├── README.md               # This file
├── LICENSE                 # MIT License
├── CONTRIBUTING.md         # Contributing guide
└── .gitignore
```

---

### Quick Start

#### Requirements

| Component | Requirement                           |
| --------- | ------------------------------------- |
| Backend   | Java 17+, Maven 3.6+                  |
| Crawler   | Node.js 18+                           |
| Browser   | Chromium (auto-installed by Playwright) |

#### 1. Start RAG Backend

```bash
cd backend
./mvnw spring-boot:run
# API runs at http://localhost:8080
```

#### 2. Start Crawler Console (Entry Point)

```bash
cd crawler
npm install
npx playwright install chromium
node server.js
# Console runs at http://localhost:3456
```

#### 3. Start Demo Site (Optional)

```bash
cd demo
node server.js
# Demo runs at http://localhost:4567
```

#### 4. Full Pipeline Verification

1. Open crawler console → Enter target URL → Click "Start Crawling"
2. After crawling completes, click "Push to RAG" → Auto-build knowledge base
3. Open demo site → Click Widget in bottom-right corner → Ask questions in natural language
4. Widget returns location results → Highlights target element / navigates to page

---

### Widget Embedding Guide

Embed FuncFind Widget in any webpage with just two lines of code:

```html
<script>
  window.__SITE_NAV_CONFIG__ = {
    apiBase: "http://localhost:8080", // RAG backend URL
    kbName: "default-kb", // Knowledge base name
    topK: 6, // Retrieval count
    title: "Site Navigator", // Widget title
    subtitle: "Ask where a feature lives",
  };
</script>
<script src="widget/widget.js"></script>
```

Widget uses Shadow DOM for style isolation and will not affect the host page styles.

---

### API Overview

#### RAG Backend (`localhost:8080`)

| Endpoint                                | Method | Description                        |
| --------------------------------------- | ------ | ---------------------------------- |
| `/api/documents/upload`                 | POST   | Upload document                    |
| `/api/knowledge-base/build`             | POST   | Build knowledge base from documents|
| `/api/knowledge-base/build-from-crawl`  | POST   | Build knowledge base from crawl results|
| `/api/chat`                             | POST   | Q&A (returns location hints)       |
| `/api/locate`                           | POST   | Report location failure events     |
| `/api/runtime-config/apply`             | POST   | Apply runtime configuration        |
| `/api/runtime-config/current`           | GET    | View current configuration         |

#### Crawler Console (`localhost:3456`)

| Endpoint                    | Method | Description              |
| --------------------------- | ------ | ------------------------ |
| `/api/crawl/start`          | POST   | Start crawling           |
| `/api/crawl/stop`           | POST   | Stop crawling            |
| `/api/status`               | GET    | Crawl status             |
| `/api/results`              | GET    | Get results              |
| `/api/rag/build-from-crawl` | POST   | Push to RAG              |
| `/api/datasets`             | GET    | List historical datasets |

Full API documentation: [docs/API_CONTRACT.md](docs/API_CONTRACT.md).

---

### Tech Stack

- **Backend**: Spring Boot 3.3, Java 17, Maven
- **Vector Store**: Cloudflare Vectorize (swappable with Pinecone / Weaviate / Milvus)
- **Embedding**: all-MiniLM-L6-v2 (384 dimensions)
- **Crawler**: Crawlee 3.x + Playwright 1.44
- **Frontend**: Vue 3 (ES Module, no build step)
- **Widget**: Vanilla JS, Shadow DOM, zero dependencies
- **Communication**: REST API, Server-Sent Events (SSE)

---

### Location Strategy

Widget element location priority:

1. **XPath** — Most precise, obtained from `locations[].xpath`
2. **CSS Selector** — Fallback, obtained from `locations[].selector`
3. **Element Text** — Final fallback, traverses page matching `elementText`

On location failure, events are automatically reported to `/api/locate` (JSONL format), including page URL, attempted selectors/XPath, failure reason, and timestamp.

---

### Documentation Index

- [Architecture Decision Records](docs/ARCHITECTURE.md)
- [API Contract](docs/API_CONTRACT.md)
- [User Operation Guide](docs/USER_GUIDE.md)
- [Contributing Guide](CONTRIBUTING.md)

---

## Demo Video / 演示视频

<video src="view.mp4" controls width="100%"></video>

> If the video does not render above, click [here to download view.mp4](view.mp4).

---

## License

[MIT](LICENSE)
