# Embedded Widget

A minimal, standalone widget for asking the RAG backend where a feature is and highlighting it on the page.

## Quick Start

1. Serve this folder with any static server.
2. Open demo.html in a browser.
3. Ask a question and use the Locate button to highlight elements.

## Embed Usage

```html
<script>
  window.__SITE_NAV_CONFIG__ = {
    apiBase: "http://localhost:8080",
    kbName: "default-kb",
    topK: 6,
    title: "Site Navigator",
    subtitle: "Ask where a feature lives",
    sessionId: "",
  };
</script>
<script src="https://your-cdn.com/widget.js"></script>
```

## Notes

- The widget uses Shadow DOM to avoid style conflicts.
- The backend must enable CORS for /api/chat.
- Location hints rely on `locations[]` or metadata inside `contextChunks[]`.
