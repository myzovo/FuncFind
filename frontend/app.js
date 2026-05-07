import { createApp, nextTick } from "https://unpkg.com/vue@3/dist/vue.esm-browser.prod.js";

const SESSION_HEADER = "X-Client-Session-Id";
const SESSION_ID_STORAGE_KEY = "docvec_client_session_id";
const RUNTIME_SNAPSHOT_STORAGE_KEY = "docvec_runtime_config_snapshots";
const CHAT_MESSAGES_STORAGE_KEY = "docvec_chat_messages";
const PERSIST_KEYS_STORAGE_KEY = "docvec_persist_keys_locally";
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
        customModelId: "",  // 自定义模型 ID
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
      // --- Settings ---
      persistKeysLocally: false,
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
    // 当用户输入 API key 时，自动切换到 external 模式
    "runtimeConfig.generation.apiKey"(newValue) {
      if (newValue && newValue.trim()) {
        // 如果用户输入了 API key 且当前是 workers-ai 模式，自动切换到 external
        if (this.runtimeConfig.generation.providerType === "workers-ai") {
          this.runtimeConfig.generation.providerType = "external";
          this.runtimeConfigMessage = "检测到 API Key 已输入，已自动切换到 external 模式。";
        }
      }
    },
  },
  async created() {
    this.ensureSessionId();
    this.loadCrawledKbs();
    // 读取 API Key 持久化开关状态
    this.persistKeysLocally = localStorage.getItem(PERSIST_KEYS_STORAGE_KEY) === "true";
    this.restoreRuntimeSnapshots();
    this.restoreRuntimeSnapshotForKb(normalizeKbName(this.settings.kbName));
    this.restoreChatMessages();
    await this.loadRuntimeConfigFromServer(true);

    // 当页面获得焦点时，自动刷新知识库列表
    window.addEventListener("focus", () => {
      this.loadCrawledKbs();
    });
    // 爬虫推送成功后自动刷新知识库列表
    window.addEventListener("rag-kb-updated", () => {
      this.loadCrawledKbs();
    });
  },
  methods: {
    // --- Chat message persistence ---
    restoreChatMessages() {
      try {
        const raw = localStorage.getItem(CHAT_MESSAGES_STORAGE_KEY);
        if (raw) {
          const parsed = JSON.parse(raw);
          if (Array.isArray(parsed) && parsed.length > 0) {
            this.messages = parsed;
            // 更新 idSeed 避免 ID 冲突
            const maxId = Math.max(...parsed.map(m => m.id || 0));
            if (maxId >= idSeed) {
              idSeed = maxId + 1;
            }
          }
        }
      } catch { /* ignore */ }
    },
    saveChatMessages() {
      try {
        localStorage.setItem(CHAT_MESSAGES_STORAGE_KEY, JSON.stringify(this.messages));
      } catch { /* ignore */ }
    },
    clearChatMessages() {
      this.messages = [
        {
          id: idSeed++,
          role: "system",
          content: "欢迎使用 DocVec Studio。上传文档并构建知识库后即可开始问答。",
        },
      ];
      this.saveChatMessages();
    },
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
    async useCrawledKb(entry) {
      const kbName = entry.kbName || entry;

      // Toggle: 如果点击的是已激活的 KB，则取消激活
      if (entry._active) {
        this.settings.kbName = DEFAULT_KB_NAME;
        this.crawledKbActive = false;
        this.crawledKbs.forEach(k => { k._active = false; });
        this.systemMessage = `已停用知识库: ${kbName}`;
        return;
      }

      this.settings.kbName = kbName;
      this.crawledKbActive = true;
      this.systemMessage = `已选择知识库: ${kbName}`;
      // Mark this KB as active
      this.crawledKbs.forEach(k => { k._active = false; });
      entry._active = true;

      // 检查后端状态，确认知识库是否真正准备好
      try {
        const response = await fetch(`${this.settings.apiBase}/runtime-config/current?kbName=${encodeURIComponent(kbName)}`, {
          method: "GET",
          headers: this.buildHeaders(),
        });
        if (response.ok) {
          this.kbReady = true;
          this.systemMessage = `已激活知识库: ${kbName}`;
        } else {
          this.kbReady = false;
          this.systemMessage = `知识库 ${kbName} 未就绪，请先构建知识库。`;
        }
      } catch {
        // 如果后端不可用，仍然设置为 ready（前端演示模式）
        this.kbReady = true;
        this.systemMessage = `已激活知识库: ${kbName}（后端未连接）`;
      }
    },
    clearCrawledKbs() {
      localStorage.removeItem("crawledKbList");
      this.crawledKbs = [];
      this.crawledKbActive = false;
      if (!this.selectedFiles.length) this.kbReady = false;
      this.systemMessage = "已清除知识库列表。";
      // 同时清除服务器端的 builtKbs 列表，防止刷新后重新拉取
      fetch("/api/rag/kbs/clear", { method: "POST" }).catch(() => {});
    },
    // --- end Crawled KB methods ---

    // --- Settings ---
    togglePersistKeys() {
      this.persistKeysLocally = !this.persistKeysLocally;
      localStorage.setItem(PERSIST_KEYS_STORAGE_KEY, String(this.persistKeysLocally));
      // 迁移现有 snapshots 到目标 storage
      this.persistRuntimeSnapshots();
      if (!this.persistKeysLocally) {
        // 关闭时清除 localStorage 中的旧数据
        localStorage.removeItem(RUNTIME_SNAPSHOT_STORAGE_KEY);
      }
    },

    // 模型和 Base URL 的映射
    getModelConfig(modelId) {
      const modelConfigs = {
        "gpt-4o-mini": {
          baseUrl: "https://api.openai.com",
          providerType: "external",
          label: "GPT-4o Mini (OpenAI)"
        },
        "deepseek-chat": {
          baseUrl: "https://api.deepseek.com",
          providerType: "external",
          label: "DeepSeek Chat"
        },
        "qwen2.5-7b-instruct": {
          baseUrl: "https://dashscope.aliyuncs.com/compatible-mode",
          providerType: "external",
          label: "Qwen 2.5 7B (通义千问)"
        },
        "claude-3-haiku": {
          baseUrl: "https://api.anthropic.com",
          providerType: "external",
          label: "Claude 3 Haiku (Anthropic)"
        },
        "llama-3-8b-instruct-q4": {
          baseUrl: "",
          providerType: "workers-ai",
          label: "Llama 3 8B (Workers AI)"
        }
      };
      return modelConfigs[modelId] || null;
    },

    // 当用户选择模型时自动更新配置
    onModelChange() {
      const modelId = this.settings.generationModel;

      if (modelId === "custom") {
        // 自定义模型：切换到 external 模式，清空 Base URL 让用户填写
        this.runtimeConfig.generation.providerType = "external";
        this.runtimeConfigMessage = "已切换到自定义模型模式，请填写模型 ID 和 Base URL。";
        return;
      }

      const config = this.getModelConfig(modelId);
      if (config) {
        // 自动更新 providerType
        this.runtimeConfig.generation.providerType = config.providerType;

        // 如果是 external 模式，自动更新 Base URL
        if (config.providerType === "external" && config.baseUrl) {
          this.runtimeConfig.generation.baseUrl = config.baseUrl;
        }

        // 更新 runtimeConfig 中的模型 ID
        this.runtimeConfig.generation.generationModelId = modelId;

        this.runtimeConfigMessage = `已选择 ${config.label}，已自动配置 Base URL 和 Provider。`;
      }
    },
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
      // 优先从 localStorage 读取（长期存储），否则从 sessionStorage
      let raw = null;
      if (this.persistKeysLocally) {
        raw = localStorage.getItem(RUNTIME_SNAPSHOT_STORAGE_KEY);
      }
      if (!raw) {
        raw = sessionStorage.getItem(RUNTIME_SNAPSHOT_STORAGE_KEY);
      }
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
      const data = JSON.stringify(this.runtimeConfigByKb);
      if (this.persistKeysLocally) {
        localStorage.setItem(RUNTIME_SNAPSHOT_STORAGE_KEY, data);
        sessionStorage.removeItem(RUNTIME_SNAPSHOT_STORAGE_KEY);
      } else {
        sessionStorage.setItem(RUNTIME_SNAPSHOT_STORAGE_KEY, data);
      }
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

      // 恢复模型选择状态
      const savedModelId = snapshot.generation?.generationModelId;
      if (savedModelId) {
        const predefinedModels = ["gpt-4o-mini", "deepseek-chat", "qwen2.5-7b-instruct", "claude-3-haiku", "llama-3-8b-instruct-q4"];
        if (predefinedModels.includes(savedModelId)) {
          this.settings.generationModel = savedModelId;
          this.settings.customModelId = "";
        } else {
          // 自定义模型
          this.settings.generationModel = "custom";
          this.settings.customModelId = savedModelId;
        }
      }
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

        // 同步模型选择状态
        const serverModelId = responseData.generation.generationModelId || "gpt-4o-mini";
        const predefinedModels = ["gpt-4o-mini", "deepseek-chat", "qwen2.5-7b-instruct", "claude-3-haiku", "llama-3-8b-instruct-q4"];
        if (predefinedModels.includes(serverModelId)) {
          this.settings.generationModel = serverModelId;
          this.settings.customModelId = "";
        } else {
          this.settings.generationModel = "custom";
          this.settings.customModelId = serverModelId;
        }
      }

      this.runtimeServerState.appliedAt = responseData.appliedAt || "";
      if (responseData.generation && responseData.generation.generationModelId) {
        this.settings.generationModel = responseData.generation.generationModelId;
      }
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

      // Sync user-selected model into runtime config before applying
      if (this.settings.generationModel === "custom") {
        // 自定义模型：使用 customModelId
        if (this.settings.customModelId) {
          this.runtimeConfig.generation.generationModelId = this.settings.customModelId;
        }
      } else if (this.settings.generationModel) {
        this.runtimeConfig.generation.generationModelId = this.settings.generationModel;
      }

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
      // 确定使用的模型：如果是自定义模型，使用 customModelId
      let generationModel;
      if (this.settings.generationModel === "custom") {
        generationModel = this.settings.customModelId || this.runtimeConfig.generation.generationModelId;
      } else {
        generationModel = this.settings.generationModel || this.runtimeConfig.generation.generationModelId;
      }
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
        const msg = {
          id: idSeed++,
          role: "assistant",
          content: data.answer || "后端未返回 answer 字段。",
          aiGenerated: data.aiGenerated !== false,
          fallbackReason: data.fallbackReason || null,
          retrievedCount: data.retrievedCount || 0,
          generationModel: data.generationModel || null,
        };
        this.messages.push(msg);
      } catch {
        const fallback = this.mockAnswer(question);
        this.messages.push({ id: idSeed++, role: "assistant", content: fallback });
      } finally {
        this.chatting = false;
        this.saveChatMessages();
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
