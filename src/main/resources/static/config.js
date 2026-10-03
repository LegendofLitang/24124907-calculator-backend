/**
 * config.js — 部署配置（前端唯一的配置项）
 *
 * 【保持 ""】默认值，适用于**前后端同源**的场景：
 *   - 由后端一并提供页面（如本项目部署在 http://111.230.148.219）→ 自动走相对路径 /api/...
 *   - 用 file:// 直接双击打开 html 文件 → 自动回退到 http://localhost:8080
 *
 * 【填写地址】适用于**前后端分离部署**，或本地用 Live Server 调试：
 *     window.API_BASE = "http://111.230.148.219";    // 指向后端公网地址
 *     window.API_BASE = "http://localhost:8080";     // 指向本地后端
 *
 * 注意：本文件必须在 app.js **之前**加载（见 index.html 底部的 script 顺序）。
 */
window.API_BASE = "";
