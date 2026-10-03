/**
 * config.js — 部署配置（前端唯一的配置项）
 *
 * 部署到公网时，把下面的值改成**后端的公网地址**，例如：
 *     window.API_BASE = "https://calculator-api.example.com";
 * 或带端口的：
 *     window.API_BASE = "http://1.2.3.4:8080";
 *
 * 留空字符串 "" 时，由 app.js 自动判断后端地址：
 *   - 页面由后端 8080 端口提供（同源）→ 走相对路径 /api/...
 *   - 其他情况（Live Server / http.server / 直接打开）→ 回退到 http://localhost:8080
 *
 * 注意：本文件必须在 app.js **之前**加载（见 index.html 底部的 script 顺序）。
 */
window.API_BASE = "";
