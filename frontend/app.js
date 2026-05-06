import { createApp, nextTick } from "https://unpkg.com/vue@3/dist/vue.esm-browser.prod.js";

const SESSION_HEADER = "X-Client-Session-Id";
const SESSION_ID_STORAGE_KEY = "docvec_client_session_id";
const RUNTIME_SNAPSHOT_STORAGE_KEY = "docvec_runtime_config_snapshots";
const DEFAULT_KB_NAME = "default-kb";

let idSeed = 1;

function normalizeKbName(kbName) {
  if (!kbName || !String(kbName).trim()) {
    return DEFAULT_KB_NAME;
  }
  return String(kbName).trim();
}

function createSessionId() {
  if (window.crypto && typeof window.crypto.randomUUID === "function") {
    return window.crypto.randomUUID();
  }
  return `sess-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function defaultRuntimeConfig() {
  return {
    cloudflare: {
      accountId: "",
      apiToken: "",
      vectorizeIndexName: "docvec-default",
      vectorizeNamespace: "default",
      r2Bucket: "docvec-raw",
    },
    generation: {
      providerType: "workers-ai",
      baseUrl: "http://localhost:9000",
      apiKey: "",
      generationModelId: "gpt-4o-mini",
    },
  };
}

function defaultRuntimeServerState() {
  return {
    appliedAt: "",
    hasCloudflareToken: false,
    cloudflareTokenMasked: "",
    hasGenerationApiKey: false,
    generationApiKeyMasked: "",
  };
}

function cloneRuntimeConfig(config) {
  return {
    cloudflare: {
      accountId: config?.cloudflare?.accountId || "",
      apiToken: config?.cloudflare?.apiToken || "",
      vectorizeIndexName: config?.cloudflare?.vectorizeIndexName || "",
      vectorizeNamespace: config?.cloudflare?.vectorizeNamespace || "",
      r2Bucket: config?.cloudflare?.r2Bucket || "",
    },
    generation: {
      providerType: config?.generation?.providerType || "workers-ai",
      baseUrl: config?.generation?.baseUrl || "",
      apiKey: config?.generation?.apiKey || "",
      generationModelId: config?.generation?.generationModelId || "gpt-4o-mini",
    },
  };
}

createApp({
  data() {
    return {
      selectedFiles: [],
      dragActive: false,
      uploading: false,
      buildingKb: false,
      chatting: false,
      kbReady: false,
      draft: "",
      systemMessage: "",
      applyingRuntimeConfig: false,
      loadingRuntimeConfig: false,
      runtimeConfigMessage: "",
      clientSessionId: "",
      runtimeConfig: defaultRuntimeConfig(),
      runtimeConfigByKb: {},
      runtimeServerState: defaultRuntimeServerState(),
      showSecrets: {
        cloudflareToken: false,
        generationApiKey: false,
      },
      pipelineState: {
        extract: "idle",
        chunk: "idle",
        embed: "idle",
        index: "idle",
      },
      settings: {
        kbName: DEFAULT_KB_NAME,
        apiBase: "http://localhost:8080/api",
        embeddingModel: "all-MiniLM-L6-v2",
        generationModel: "gpt-4o-mini",
        chunkSize: 500,
        semanticThreshold: 0.78,
        topK: 6,
      },
      messages: [
        {
          id: idSeed++,
          role: "system",
          content: "欢迎使用 DocVec Studio。上传文档并构建知识库后即可开始问答。",
        },
      ],
      // --- Crawled KB support ---
      crawledKbs: [],
      crawledKbActive: false,
      // --- Chat settings ---
      showChatSettings: false,
    };
  },
  computed: {
    clientSessionIdShort() {
      if (!this.clientSessionId) {
        return "-";
      }
      if (this.clientSessionId.length <= 12) {
        return this.clientSessionId;
      }
      return `${this.clientSessionId.slice(0, 8)}...`;
    },
  },
  watch: {
    "settings.kbName"(newValue, oldValue) {
      const oldKb = normalizeKbName(oldValue);
      const newKb = normalizeKbName(newValue);
      if (oldKb === newKb) {
        return;
      }

      this.persistRuntimeSnapshot(oldKb);
      this.restoreRuntimeSnapshotForKb(newKb);
      this.runtimeConfigMessage = `已切换到知识库 ${newKb} 的会话配置快照。`;
      this.loadRuntimeConfigFromServer(true);
    },
  },
  async created() {
    this.ensureSessionId();
    this.loadCrawledKbs();
    this.restoreRuntimeSnapshots();
    this.restoreRuntimeSnapshotForKb(normalizeKbName(this.settings.kbName));
    await this.loadRuntimeConfigFromServer(true);
  },
  methods: {
    // --- Crawled KB methods ---
    loadCrawledKbs() {
      // 1. Load from localStorage (persisted across browser sessions)
      try {
        const raw = localStorage.getItem("crawledKbList");
        if (raw) {
          const parsed = JSON.parse(raw);
          if (Array.isArray(parsed)) this.crawledKbs = parsed;
        }
      } catch { /* ignore */ }

      // Restore active state based on current kbName
      this.restoreActiveKbState();

      // 2. Also fetch from console server to get KBs built this session
      fetch("/api/rag/kbs")
        .then(r => r.json())
        .then(data => {
          if (data.kbs && Array.isArray(data.kbs)) {
            // Merge: server KBs + localStorage KBs, dedupe by kbName
            const merged = [...this.crawledKbs];
            for (const skb of data.kbs) {
              if (!merged.find(m => m.kbName === skb.kbName)) {
                merged.push(skb);
              } else {
                const idx = merged.findIndex(m => m.kbName === skb.kbName);
                if (idx >= 0) merged[idx] = { ...merged[idx], ...skb };
              }
            }
            this.crawledKbs = merged;
            // Sync back to localStorage
            localStorage.setItem("crawledKbList", JSON.stringify(merged));
            // Restore active state after merge
            this.restoreActiveKbState();
          }
        })
        .catch(() => { /* server not available, use localStorage only */ });
    },
    restoreActiveKbState() {
      const currentKb = normalizeKbName(this.settings.kbName);
      this.crawledKbs.forEach(k => {
        k._active = normalizeKbName(k.kbName) === currentKb;
      });
    },
    useCrawledKb(entry) {
      const kbName = entry.kbName || entry;
      this.settings.kbName = kbName;
      this.kbReady = true;
      this.crawledKbActive = true;
      this.systemMessage = `已激活知识库: ${kbName}`;
      // Mark this KB as active
      this.crawledKbs.forEach(k => { k._active = false; });
      entry._active = true;
    },
    clearCrawledKbs() {
      localStorage.removeItem("crawledKbList");
      this.crawledKbs = [];
      this.crawledKbActive = false;
      if (!this.selectedFiles.length) this.kbReady = false;
      this.systemMessage = "已清除知识库列表。";
    },
    // --- end Crawled KB methods ---
    openFilePicker() {
      this.$refs.fileInput.click();
    },
    handleFileChoose(event) {
      const files = Array.from(event.target.files || []);
      this.appendFiles(files);
      event.target.value = "";
    },
    handleDrop(event) {
      this.dragActive = false;
      const files = Array.from(event.dataTransfer?.files || []);
      this.appendFiles(files);
    },
    appendFiles(files) {
      const picked = files
        .filter((f) => /\.(pdf|docx|txt)$/i.test(f.name))
        .map((f) => ({
          id: `${Date.now()}-${idSeed++}`,
          name: f.name,
          size: f.size,
          raw: f,
        }));

      const existingNames = new Set(this.selectedFiles.map((f) => f.name));
      const deduped = picked.filter((f) => !existingNames.has(f.name));
      this.selectedFiles = [...this.selectedFiles, ...deduped];

      if (picked.length !== deduped.length) {
        this.systemMessage = "已自动忽略同名文件。";
      }
    },
    removeFile(id) {
      this.selectedFiles = this.selectedFiles.filter((f) => f.id !== id);
      if (!this.selectedFiles.length) {
        this.kbReady = false;
      }
    },
    resetPipeline() {
      this.pipelineState = { extract: "idle", chunk: "idle", embed: "idle", index: "idle" };
    },
    ensureSessionId() {
      const existing = sessionStorage.getItem(SESSION_ID_STORAGE_KEY);
      if (existing && existing.trim()) {
        this.clientSessionId = existing;
        return;
      }
      const generated = createSessionId();
      sessionStorage.setItem(SESSION_ID_STORAGE_KEY, generated);
      this.clientSessionId = generated;
    },
    currentKbName() {
      const normalizedKbName = normalizeKbName(this.settings.kbName);
      if (this.settings.kbName !== normalizedKbName) {
        this.settings.kbName = normalizedKbName;
      }
      return normalizedKbName;
    },
    buildHeaders(additionalHeaders = {}) {
      return {
        ...additionalHeaders,
        [SESSION_HEADER]: this.clientSessionId,
      };
    },
    restoreRuntimeSnapshots() {
      const raw = sessionStorage.getItem(RUNTIME_SNAPSHOT_STORAGE_KEY);
      if (!raw) {
        this.runtimeConfigByKb = {};
        return;
      }

      try {
        const parsed = JSON.parse(raw);
        this.runtimeConfigByKb = parsed && typeof parsed === "object" ? parsed : {};
      } catch {
        this.runtimeConfigByKb = {};
      }
    },
    persistRuntimeSnapshots() {
      sessionStorage.setItem(RUNTIME_SNAPSHOT_STORAGE_KEY, JSON.stringify(this.runtimeConfigByKb));
    },
    persistRuntimeSnapshot(kbName) {
      const key = normalizeKbName(kbName);
      this.runtimeConfigByKb[key] = cloneRuntimeConfig(this.runtimeConfig);
      this.persistRuntimeSnapshots();
    },
    restoreRuntimeSnapshotForKb(kbName) {
      const key = normalizeKbName(kbName);
      const snapshot = this.runtimeConfigByKb[key];
      if (!snapshot) {
        this.runtimeConfig = defaultRuntimeConfig();
        this.runtimeServerState = defaultRuntimeServerState();
        return;
      }

      const merged = defaultRuntimeConfig();
      merged.cloudflare.accountId = snapshot.cloudflare?.accountId || merged.cloudflare.accountId;
      merged.cloudflare.apiToken = snapshot.cloudflare?.apiToken || "";
      merged.cloudflare.vectorizeIndexName =
        snapshot.cloudflare?.vectorizeIndexName || merged.cloudflare.vectorizeIndexName;
      merged.cloudflare.vectorizeNamespace =
        snapshot.cloudflare?.vectorizeNamespace || merged.cloudflare.vectorizeNamespace;
      merged.cloudflare.r2Bucket = snapshot.cloudflare?.r2Bucket || merged.cloudflare.r2Bucket;

      merged.generation.providerType = snapshot.generation?.providerType || merged.generation.providerType;
      merged.generation.baseUrl = snapshot.generation?.baseUrl || merged.generation.baseUrl;
      merged.generation.apiKey = snapshot.generation?.apiKey || "";
      merged.generation.generationModelId =
        snapshot.generation?.generationModelId || merged.generation.generationModelId;

      this.runtimeConfig = merged;
      this.runtimeServerState = defaultRuntimeServerState();
    },
    mergeRuntimeFromServer(responseData) {
      if (!responseData || typeof responseData !== "object") {
        return;
      }

      if (responseData.cloudflare) {
        this.runtimeConfig.cloudflare.accountId = responseData.cloudflare.accountId || "";
        this.runtimeConfig.cloudflare.vectorizeIndexName = responseData.cloudflare.vectorizeIndexName || "";
        this.runtimeConfig.cloudflare.vectorizeNamespace = responseData.cloudflare.vectorizeNamespace || "";
        this.runtimeConfig.cloudflare.r2Bucket = responseData.cloudflare.r2Bucket || "";

        this.runtimeServerState.hasCloudflareToken = Boolean(responseData.cloudflare.hasApiToken);
        this.runtimeServerState.cloudflareTokenMasked = responseData.cloudflare.apiTokenMasked || "";

        if (!responseData.cloudflare.hasApiToken) {
          this.runtimeConfig.cloudflare.apiToken = "";
        }
      }

      if (responseData.generation) {
        this.runtimeConfig.generation.providerType = responseData.generation.providerType || "workers-ai";
        this.runtimeConfig.generation.baseUrl = responseData.generation.baseUrl || "";
        this.runtimeConfig.generation.generationModelId = responseData.generation.generationModelId || "gpt-4o-mini";

        this.runtimeServerState.hasGenerationApiKey = Boolean(responseData.generation.hasApiKey);
        this.runtimeServerState.generationApiKeyMasked = responseData.generation.apiKeyMasked || "";

        if (!responseData.generation.hasApiKey) {
          this.runtimeConfig.generation.apiKey = "";
        }
      }

      this.runtimeServerState.appliedAt = responseData.appliedAt || "";
      this.settings.generationModel = this.runtimeConfig.generation.generationModelId || this.settings.generationModel;
      this.persistRuntimeSnapshot(this.currentKbName());
    },
    async loadRuntimeConfigFromServer(silent = false) {
      this.loadingRuntimeConfig = true;

      const kbName = this.currentKbName();
      const params = new URLSearchParams({ kbName });

      try {
        const response = await fetch(`${this.settings.apiBase}/runtime-config/current?${params.toString()}`, {
          method: "GET",
          headers: this.buildHeaders(),
        });

        if (!response.ok) {
          throw new Error("runtime config current failed");
        }

        const data = await response.json();
        this.mergeRuntimeFromServer(data);
        if (!silent) {
          this.runtimeConfigMessage = data.appliedAt
            ? `已读取 ${kbName} 的会话配置。`
            : `当前 ${kbName} 尚未显式应用会话配置，已使用默认值。`;
        }
      } catch {
        if (!silent) {
          this.runtimeConfigMessage = "未连接到后端 runtime-config 接口，当前使用本地会话快照。";
        }
      } finally {
        this.loadingRuntimeConfig = false;
      }
    },
    async applyRuntimeConfig() {
      this.applyingRuntimeConfig = true;

      const kbName = this.currentKbName();
      this.persistRuntimeSnapshot(kbName);

      const payload = {
        kbName,
        cloudflare: {
          accountId: this.runtimeConfig.cloudflare.accountId,
          apiToken: this.runtimeConfig.cloudflare.apiToken,
          vectorizeIndexName: this.runtimeConfig.cloudflare.vectorizeIndexName,
          vectorizeNamespace: this.runtimeConfig.cloudflare.vectorizeNamespace,
          r2Bucket: this.runtimeConfig.cloudflare.r2Bucket,
        },
        generation: {
          providerType: this.runtimeConfig.generation.providerType,
          baseUrl: this.runtimeConfig.generation.baseUrl,
          apiKey: this.runtimeConfig.generation.apiKey,
          generationModelId: this.runtimeConfig.generation.generationModelId,
        },
      };

      try {
        const response = await fetch(`${this.settings.apiBase}/runtime-config/apply`, {
          method: "POST",
          headers: this.buildHeaders({ "Content-Type": "application/json" }),
          body: JSON.stringify(payload),
        });

        if (!response.ok) {
          throw new Error("runtime config apply failed");
        }

        const data = await response.json();
        this.mergeRuntimeFromServer(data);
        this.runtimeConfigMessage = `会话配置已应用到 ${kbName}。后续业务请求将自动携带会话头。`;
      } catch {
        this.runtimeConfigMessage = "会话配置应用失败，请检查 API Base URL 与后端状态。";
      } finally {
        this.applyingRuntimeConfig = false;
      }
    },
    async uploadDocuments() {
      if (!this.selectedFiles.length) return;

      this.uploading = true;
      this.systemMessage = "正在上传文档...";

      const kbName = this.currentKbName();
      this.persistRuntimeSnapshot(kbName);

      try {
        for (const file of this.selectedFiles) {
          const formData = new FormData();
          formData.append("file", file.raw);
          formData.append("kbName", kbName);

          await fetch(`${this.settings.apiBase}/documents/upload`, {
            method: "POST",
            headers: this.buildHeaders(),
            body: formData,
          });
        }
        this.systemMessage = "文档上传完成。";
      } catch (error) {
        this.systemMessage = "未连接到后端，已在前端暂存文件。稍后可直接执行知识库构建。";
      } finally {
        this.uploading = false;
      }
    },
    async buildKnowledgeBase() {
      if (!this.selectedFiles.length) return;

      this.buildingKb = true;
      this.kbReady = false;
      this.systemMessage = "开始构建知识库...";
      this.resetPipeline();

      const kbName = this.currentKbName();
      this.persistRuntimeSnapshot(kbName);

      try {
        this.pipelineState.extract = "active";
        await this.wait(450);

        this.pipelineState.extract = "done";
        this.pipelineState.chunk = "active";
        await this.wait(450);

        this.pipelineState.chunk = "done";
        this.pipelineState.embed = "active";
        await this.wait(450);

        this.pipelineState.embed = "done";
        this.pipelineState.index = "active";

        const payload = {
          kbName,
          chunkSize: this.settings.chunkSize,
          semanticThreshold: this.settings.semanticThreshold,
          topK: this.settings.topK,
          embeddingModel: this.settings.embeddingModel,
          sourceDocs: this.selectedFiles.map((f, idx) => ({
            sourceDocId: `doc-${idx + 1}`,
            filename: f.name,
            size: f.size,
          })),
        };

        try {
          await fetch(`${this.settings.apiBase}/knowledge-base/build`, {
            method: "POST",
            headers: this.buildHeaders({ "Content-Type": "application/json" }),
            body: JSON.stringify(payload),
          });
        } catch {
          // Keep the frontend usable without backend during early bootstrapping.
        }

        await this.wait(450);
        this.pipelineState.index = "done";
        this.kbReady = true;
        this.systemMessage = `知识库 ${kbName} 构建完成。`;

        // Track this KB in the knowledge base list
        const entry = {
          kbName,
          dataset: "文档上传",
          pushedAt: new Date().toISOString(),
          source: "upload",
        };
        const existing = this.crawledKbs.findIndex(k => k.kbName === kbName);
        if (existing >= 0) {
          this.crawledKbs[existing] = entry;
        } else {
          this.crawledKbs.push(entry);
        }
        localStorage.setItem("crawledKbList", JSON.stringify(this.crawledKbs));
      } finally {
        this.buildingKb = false;
      }
    },
    async sendMessage() {
      const question = this.draft.trim();
      if (!question) return;

      this.messages.push({ id: idSeed++, role: "user", content: question });
      this.draft = "";
      this.chatting = true;
      await this.scrollChatToBottom();

      const kbName = this.currentKbName();
      const generationModel = this.settings.generationModel || this.runtimeConfig.generation.generationModelId;
      const payload = {
        question,
        kbName,
        topK: this.settings.topK,
        generationModel,
      };

      try {
        const response = await fetch(`${this.settings.apiBase}/chat`, {
          method: "POST",
          headers: this.buildHeaders({ "Content-Type": "application/json" }),
          body: JSON.stringify(payload),
        });

        if (!response.ok) throw new Error("chat api error");

        const data = await response.json();
        this.messages.push({
          id: idSeed++,
          role: "assistant",
          content: data.answer || "后端未返回 answer 字段。",
        });
      } catch {
        const fallback = this.mockAnswer(question);
        this.messages.push({ id: idSeed++, role: "assistant", content: fallback });
      } finally {
        this.chatting = false;
        await this.scrollChatToBottom();
      }
    },
    mockAnswer(question) {
      const docs = this.selectedFiles.slice(0, 3).map((f) => f.name).join("、") || "暂无文档";
      return [
        "当前为前端演示回答（后端未连接）。",
        `你的问题：${question}`,
        `已关联文档：${docs}`,
        "建议下一步：启动后端接口 /documents/upload、/knowledge-base/build、/chat 后即可返回真实检索增强答案。",
      ].join("\n");
    },
    formatBytes(size) {
      if (!size && size !== 0) return "0 B";
      const units = ["B", "KB", "MB", "GB"];
      let value = size;
      let unit = 0;
      while (value >= 1024 && unit < units.length - 1) {
        value /= 1024;
        unit += 1;
      }
      return `${value.toFixed(value > 10 ? 0 : 1)} ${units[unit]}`;
    },
    wait(ms) {
      return new Promise((resolve) => setTimeout(resolve, ms));
    },
    async scrollChatToBottom() {
      await nextTick();
      const chatFeed = this.$refs.chatFeed;
      if (chatFeed) {
        chatFeed.scrollTop = chatFeed.scrollHeight;
      }
    },
  },
}).mount("#app");
