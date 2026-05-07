import http from "node:http";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const publicDir = path.join(__dirname, "public");
const widgetPath = path.join(__dirname, "..", "widget", "widget.js");
const PORT = process.env.PORT || 4567;

const MIME_TYPES = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".svg": "image/svg+xml",
  ".ico": "image/x-icon",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
};

const server = http.createServer((req, res) => {
  const url = new URL(req.url || "/", `http://${req.headers.host}`);
  let pathname = url.pathname || "/";

  if (pathname === "/") {
    pathname = "/index.html";
  }

  if (pathname === "/widget.js") {
    return sendFile(widgetPath, res, "application/javascript");
  }

  const filePath = path.join(publicDir, pathname);
  if (!filePath.startsWith(publicDir)) {
    res.writeHead(403);
    return res.end("Forbidden");
  }

  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME_TYPES[ext] || "application/octet-stream";
  return sendFile(filePath, res, contentType);
});

server.listen(PORT, () => {
  console.log(`Demo site running at http://localhost:${PORT}`);
});

function sendFile(filePath, res, contentType) {
  const stream = fs.createReadStream(filePath);
  stream.on("error", () => {
    if (!res.headersSent) {
      res.writeHead(404);
    }
    res.end("Not found");
  });
  res.writeHead(200, { "Content-Type": contentType });
  stream.pipe(res);
}
