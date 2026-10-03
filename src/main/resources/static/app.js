/**
 * app.js — 计算器前端逻辑
 * 连接 Spring Boot 后端 REST API，无外部依赖。
 */
(function () {
  "use strict";
  var $ = function (sel) { return document.querySelector(sel); };
  var expressionInput = $("#expression");
  var calcBtn = $("#calcBtn");
  var resultBox = $("#resultBox");
  var resultValue = $("#resultValue");
  var resultExpr = $("#resultExpr");
  var errorBox = $("#errorBox");
  var toast = $("#toast");
  var toastTimer = null;
  // 分页状态
  var currentPage = 0;
  var pageSize = 10;
  var totalPages = 0;
  // ---------------- 工具 ----------------
  function showToast(msg) {
    toast.textContent = msg;
    toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { toast.hidden = true; }, 4000);
  }
  function showError(msg) {
    errorBox.textContent = msg;
    errorBox.hidden = false;
  }
  function hideError() {
    errorBox.hidden = true;
  }
  function showResult(expr, value) {
    resultValue.textContent = value;
    resultExpr.textContent = expr + " = " + value;
    resultBox.hidden = false;
  }
  function fmtTime(iso) {
    try {
      return new Date(iso).toLocaleString("zh-CN", { hour12: false });
    } catch (e) {
      return iso || "";
    }
  }
  // ---------------- 历史渲染 ----------------
  var listEl = $("#historyList");
  var emptyEl = $("#historyEmpty");
  var loadEl = $("#historyLoad");
  var metaEl = $("#historyMeta");
  var prevBtn = $("#prevPage");
  var nextBtn = $("#nextPage");
  function renderHistory(pageData) {
    listEl.innerHTML = "";
    var items = pageData.content || [];
    emptyEl.hidden = items.length > 0;
    loadEl.hidden = true;
    metaEl.textContent = "共 " + pageData.totalElements + " 条 · 第 " +
        (pageData.page + 1) + "/" + Math.max(pageData.totalPages, 1) + " 页";
    items.forEach(function (item) {
      var li = document.createElement("li");
      li.className = "history-item" + (item.success ? "" : " failed");
      var status = document.createElement("span");
      status.className = "history-status" + (item.success ? "" : " failed");
      var main = document.createElement("div");
      main.className = "history-main";
      var expr = document.createElement("div");
      expr.className = "history-expr";
      expr.textContent = item.expression;
      expr.title = item.expression;
      var res = document.createElement("div");
      res.className = "history-result" + (item.success ? "" : " failed");
      res.textContent = item.success ? ("= " + item.result) : ("✗ " + item.errorMessage);
      main.appendChild(expr);
      main.appendChild(res);
      var time = document.createElement("span");
      time.className = "history-time";
      time.textContent = fmtTime(item.calculatedAt);
      var del = document.createElement("button");
      del.className = "del-btn";
      del.textContent = "✕";
      del.title = "删除此记录";
      del.setAttribute("aria-label", "删除记录 " + item.expression);
      del.addEventListener("click", function (ev) {
        ev.stopPropagation();
        removeRecord(item.id);
      });
      // 点击历史条目：重新查看表达式（回填到输入框并立即计算）
      li.addEventListener("click", function () {
        expressionInput.value = item.expression;
        hideError();
        resultBox.hidden = true;
        expressionInput.focus();
      });
      li.appendChild(status);
      li.appendChild(main);
      li.appendChild(time);
      li.appendChild(del);
      listEl.appendChild(li);
    });
    prevBtn.disabled = !pageData.first;
    nextBtn.disabled = pageData.last || items.length === 0;
    currentPage = pageData.page;
    totalPages = pageData.totalPages;
  }
  function loadHistory() {
    loadEl.hidden = false;
    fetch("/api/calculations?page=" + currentPage + "&size=" + pageSize)
        .then(function (res) {
          if (!res.ok) {
            return res.json().then(function (d) {
              throw new Error((d && d.message) || ("HTTP " + res.status));
            });
          }
          return res.json();
        })
        .then(renderHistory)
        .catch(function (err) {
          loadEl.hidden = true;
          showToast("加载历史失败：" + err.message);
        });
  }
  function removeRecord(id) {
    fetch("/api/calculations/" + id, { method: "DELETE" })
        .then(function (res) {
          if (!res.ok) {
            return res.json().then(function (d) {
              throw new Error((d && d.message) || ("HTTP " + res.status));
            });
          }
          loadHistory();
        })
        .catch(function (err) {
          showToast("删除失败：" + err.message);
        });
  }
  // ---------------- 计算 ----------------
  function calculate(expression) {
    hideError();
    resultBox.hidden = true;
    calcBtn.disabled = true;
    $("#calcBtnText").textContent = "计算中…";
    fetch("/api/calculations", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ expression: expression })
    })
        .then(function (res) {
          return res.json().then(function (data) {
            if (!res.ok) {
              var err = new Error((data && data.message) || ("HTTP " + res.status));
              err.status = res.status;
              throw err;
            }
            return data;
          });
        })
        .then(function (data) {
          if (data.success) {
            showResult(data.expression, data.result);
            // 回到第一页查看最新记录
            currentPage = 0;
            loadHistory();
          } else {
            showError(data.errorMessage || "计算失败");
            loadHistory();
          }
        })
        .catch(function (err) {
          showError(err.message || "计算失败，请检查表达式");
        })
        .finally(function () {
          calcBtn.disabled = false;
          $("#calcBtnText").textContent = "计算";
        });
  }
  // ---------------- 事件绑定 ----------------
  $("#calcForm").addEventListener("submit", function (e) {
    e.preventDefault();
    var expr = expressionInput.value.trim();
    if (!expr) {
      showError("表达式不能为空");
      return;
    }
    calculate(expr);
  });
  $("#clearInput").addEventListener("click", function () {
    expressionInput.value = "";
    hideError();
    resultBox.hidden = true;
    expressionInput.focus();
  });
  $("#clearHistory").addEventListener("click", function () {
    if (!confirm("确定清空全部历史记录吗？")) return;
    fetch("/api/calculations", { method: "DELETE" })
        .then(function (res) {
          if (!res.ok) {
            return res.json().then(function (d) {
              throw new Error((d && d.message) || ("HTTP " + res.status));
            });
          }
          currentPage = 0;
          loadHistory();
        })
        .catch(function (err) {
          showToast("清空失败：" + err.message);
        });
  });
  prevBtn.addEventListener("click", function () {
    if (currentPage > 0) { currentPage--; loadHistory(); }
  });
  nextBtn.addEventListener("click", function () {
    if (currentPage + 1 < totalPages) { currentPage++; loadHistory(); }
  });
  // 回车即计算
  expressionInput.addEventListener("keydown", function (e) {
    if (e.key === "Enter") { e.preventDefault(); $("#calcForm").dispatchEvent(new Event("submit")); }
  });

  // ===== 虚拟按键：点击输入 + 退格【新增】=====
  // 在光标处插入字符；若浏览器不提供光标位置，则退化为追加到末尾。
  function insertIntoInput(text) {
    hideError();
    resultBox.hidden = true;
    var value = expressionInput.value;
    var start = expressionInput.selectionStart;
    var end = expressionInput.selectionEnd;
    if (typeof start !== "number" || typeof end !== "number") {
      expressionInput.value = value + text;
      return;
    }
    expressionInput.value = value.slice(0, start) + text + value.slice(end);
    var caret = start + text.length;
    expressionInput.setSelectionRange(caret, caret);
  }

  // 退格：删除光标前一个字符；若有选区则删除选中内容。
  function backspaceInput() {
    hideError();
    resultBox.hidden = true;
    var value = expressionInput.value;
    var start = expressionInput.selectionStart;
    var end = expressionInput.selectionEnd;
    if (typeof start !== "number" || typeof end !== "number") {
      expressionInput.value = value.slice(0, -1);
      return;
    }
    if (start === end) {
      if (start === 0) { return; }
      start = start - 1;
    }
    expressionInput.value = value.slice(0, start) + value.slice(end);
    expressionInput.setSelectionRange(start, start);
  }

  // 事件委托：整块面板只绑一次；元素缺失时安全跳过，不会抛错。
  var keypadEl = document.querySelector(".calc-keypad");
  if (keypadEl) {
    keypadEl.addEventListener("click", function (ev) {
      var target = ev.target;
      var btn = target && target.closest ? target.closest(".key-btn") : null;
      if (!btn || !keypadEl.contains(btn)) { return; }
      var key = btn.getAttribute("data-key");
      if (!key) { return; }
      if (key === "back") {
        backspaceInput();
      } else if (key === "clear") {
        expressionInput.value = "";
        hideError();
        resultBox.hidden = true;
      } else {
        insertIntoInput(key);
      }
      expressionInput.focus();
    });
  }

  // ---------------- 启动 ----------------
  loadHistory();
})();
