# 部署指南

本文件说明如何把计算器系统部署到公网，供助教在评测期间访问。

> **重要：H2 是文件型数据库**，数据写在程序运行目录的 `./data/` 下。
> 因此部署时必须保证该目录**位于持久存储**上，否则历史记录会在重启后丢失，
> 不满足作业「刷新、重启后历史数据不丢失」的要求。

---

## 目录

- [方案对比与选型](#方案对比与选型)
- [方案 A：国内云服务器（推荐）](#方案-a国内云服务器推荐)
- [方案 B：容器平台](#方案-b容器平台)
- [方案 C：内网穿透](#方案-c内网穿透)
- [前端部署](#前端部署)
- [部署后验证清单](#部署后验证清单)
- [常见问题](#常见问题)

---

## 方案对比与选型

| 方案 | 成本 | 国内访问速度 | H2 数据持久性 | 评测期稳定性 | 推荐度 |
| ---- | ---- | ---- | ---- | ---- | ---- |
| A. 国内云服务器 | ¥10–40/月（学生机更便宜） | 快，无需代理 | 服务器磁盘，天然持久 | 高 | ⭐⭐⭐⭐⭐ |
| B. 容器平台（Railway / Render / Koyeb） | 免费额度 | 较慢，可能需要代理 | 需挂载持久卷，免费版常不支持 | 中 | ⭐⭐⭐ |
| C. 内网穿透（cpolar / 花生壳 / frp） | 免费额度 | 一般 | 本地磁盘，持久 | **低：电脑必须一直开机** | ⭐⭐ |

**选型建议**：

- 追求**稳定**、能接受每月十几块 → **方案 A**
- 完全不想花钱、能接受访问慢 → 方案 B
- 只是临时演示、电脑可以一直开着 → 方案 C

> 💡 **简化技巧**：本后端在 `src/main/resources/static/` 下内置了前端的副本，
> 所以**只部署后端，前后端就都能访问**（浏览器打开 `http://<地址>:8080` 即是完整系统）。
> 前端仓库仍然独立存在，满足作业「两个独立仓库」的要求。
> 如果想让前端也独立部署（更能体现前后端分离），见[前端部署](#前端部署)。

---

## 方案 A：国内云服务器（推荐）

### 0. 准备

- 一台云服务器：阿里云 / 腾讯云 **轻量应用服务器**
  - 学生认证后可享优惠价；配置 **2 核 2G** 足够
  - 操作系统选 **Ubuntu 22.04 LTS**
  - 记下：公网 IP、root 密码（或 SSH 密钥）
- 本地已安装 **JDK 17** 与 **Maven**

> ⚠️ 注意：本机 PATH 里的默认 `java` 可能是 Java 8，**Spring Boot 3 需要 JDK 17**。
> 用 `java -version` 确认，不是 17 就显式指定 JDK 17 的路径。

### 1. 本地打包

```bash
cd 24124907-calculator-backend
mvn clean package
```

产物：`target/calculator-system-1.0.0.jar`（约 20–30 MB，内含所有依赖，可直接运行）

跳过测试可加快速度：`mvn clean package -DskipTests`

### 2. 上传到服务器

```bash
scp target/calculator-system-1.0.0.jar root@<服务器公网IP>:/root/
```

Windows 下也可用 WinSCP、MobaXterm 等图形工具拖拽上传。

### 3. 服务器安装 JDK 17

SSH 登录服务器后执行：

```bash
ssh root@<服务器公网IP>
apt update
apt install -y openjdk-17-jdk
java -version     # 应显示 17.x
```

### 4. 启动服务

```bash
cd /root
mkdir -p /root/calculator && mv calculator-system-1.0.0.jar /root/calculator/
cd /root/calculator

# 后台启动，日志写入 app.log
nohup java -jar calculator-system-1.0.0.jar > app.log 2>&1 &

# 等待几秒后查看日志，确认出现 "Started CalculatorApplication"
tail -f app.log
```

> H2 数据文件会自动创建在 `/root/calculator/data/`，即持久磁盘，重启不丢。

### 5. 放行端口

**必须在云厂商控制台配置安全组**，否则外网访问不到：

| 方向 | 协议 | 端口 | 来源 |
| ---- | ---- | ---- | ---- |
| 入方向 | TCP | **8080** | 0.0.0.0/0 |
| 入方向 | TCP | 22 | 你的 IP（SSH 用） |

阿里云：轻量应用服务器 → 防火墙 → 添加规则
腾讯云：轻量应用服务器 → 防火墙 → 添加规则

> 如果服务器本身开了 ufw，还需 `ufw allow 8080`。

### 6. 验证

浏览器打开：

```
http://<服务器公网IP>:8080
```

应看到计算器界面。测试：

```bash
# 在任意机器上执行，应返回 JSON 结果
curl -X POST http://<服务器公网IP>:8080/api/calculations \
     -H "Content-Type: application/json" \
     -d '{"expression":"(1+2)*3"}'
# 期望: {"id":1,"expression":"(1+2)*3","result":9.0,"success":true,...}
```

### 7. 配置开机自启（可选但推荐）

新建 `/etc/systemd/system/calculator.service`：

```ini
[Unit]
Description=Calculator Backend
After=network.target

[Service]
Type=simple
WorkingDirectory=/root/calculator
ExecStart=/usr/bin/java -Xmx256m -jar /root/calculator/calculator-system-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启用：

```bash
systemctl daemon-reload
systemctl enable calculator
systemctl start calculator
systemctl status calculator      # 查看状态
journalctl -u calculator -f      # 查看日志
```

这样服务器重启后服务会自动拉起，评测期间不会掉线。

### 8. 常用运维命令

```bash
# 查看进程
ps aux | grep calculator-system

# 停止服务（用 nohup 方式启动时）
pkill -f calculator-system-1.0.0.jar

# 查看日志
tail -100 /root/calculator/app.log

# 重启服务（systemd 方式）
systemctl restart calculator
```

---

## 方案 B：容器平台

本仓库已提供 `Dockerfile`，支持 Railway / Render / Koyeb / Fly.io 等平台一键部署。

### 通用步骤

1. 在平台注册账号，选择 **Deploy from GitHub repo**
2. 选择仓库 `24124907-calculator-backend`
3. 平台会自动识别 `Dockerfile` 并构建
4. 记下平台分配的访问域名

### ⚠️ 必须配置持久卷

容器平台的文件系统默认是**临时的**，容器重建后 `/app/data` 会被清空，历史记录全部丢失。

| 平台 | 持久卷 | 说明 |
| ---- | ---- | ---- |
| Railway | 支持 Volume（免费额度有限） | 挂载到 `/app/data` |
| Render | 磁盘功能**仅付费版**支持 | 免费版无法持久化，需注意 |
| Koyeb | 部分支持 | 查阅当前文档 |
| Fly.io | 支持 Volumes | `fly volumes create calc_data --size 1` |

**如果平台不支持持久卷**，两个替代方案：

1. 把数据库改为外部托管（如免费的 PostgreSQL），需改 `pom.xml` 依赖与 `application.yml`
2. 在博客中说明该限制，用方案 A 的云服务器地址作为评测地址

### Docker 本地验证（如已安装 Docker）

```bash
docker build -t calculator-backend .
docker run -d --name calc -p 8080:8080 -v calc-data:/app/data calculator-backend
# 访问 http://localhost:8080
```

---

## 方案 C：内网穿透

适合"本机跑服务 + 映射到公网"的临时方案。

1. 注册内网穿透服务（cpolar / 花生壳 / natapp 等）
2. 本机启动后端：`java -jar target/calculator-system-1.0.0.jar`
3. 配置隧道，把本地 **8080** 端口映射出去
4. 得到形如 `http://xxxx.cpolar.cn` 的公网地址

⚠️ **重要限制**：评测期间**这台电脑必须保持开机、且服务保持运行**，否则地址失效。
建议配合 systemd / 计划任务保证稳定，或改用方案 A。

---

## 前端部署

前端是纯静态页面，可部署到免费静态托管平台。

### 步骤

1. **部署前端仓库**
   | 平台 | 操作 |
   | ---- | ---- |
   | Vercel | 导入仓库 → Framework 选 `Other` → Build Command 留空 → Output Directory 填 `.` → Deploy |
   | Netlify | 拖拽整个文件夹到 Netlify Drop，或连接仓库后 Publish directory 填 `.` |
   | Cloudflare Pages | 连接仓库 → 构建命令留空 → 输出目录 `.` |
   | GitHub Pages | 仓库 Settings → Pages → Source 选 `main` 分支根目录 |

2. **配置后端地址**（关键步骤）

   编辑前端仓库的 `config.js`，填入后端公网地址：

   ```javascript
   window.API_BASE = "http://<后端公网IP>:8080";
   ```

3. 提交并推送：

   ```bash
   git add config.js
   git commit -m "chore: 配置生产环境后端地址"
   git push
   ```

4. 平台会自动重新部署，打开前端地址即可使用。

> **跨域问题**：后端 `WebConfig` 已对 `/api/**` 放开 CORS（`allowedOriginPatterns("*")`），
> 前端部署到任何域名都能直接调用，无需额外配置。

---

## 部署后验证清单

部署完成后逐项核对：

- [ ] 浏览器能打开 `http://<后端地址>:8080`，看到计算器界面
- [ ] 计算 `12 + 8` 返回 `20`
- [ ] 计算 `(1+2)*3` 返回 `9`（验证括号与优先级）
- [ ] 计算 `1 / 0` 显示「除数不能为零」（验证异常处理）
- [ ] 计算 `1 + abc` 显示「不支持的字符」（验证非法输入）
- [ ] 历史记录列表能看到刚才的计算
- [ ] **刷新页面**后历史记录仍在
- [ ] **重启后端服务**后历史记录仍在（验证数据库持久化）
- [ ] 删除单条记录后列表立即刷新
- [ ] 清空全部历史后列表为空
- [ ] **关闭后端服务后，前端无法算出新结果**（验证前后端分离）
- [ ] 把公网地址填入两个仓库的 README 和作业博客

---

## 常见问题

| 现象 | 原因 | 解决 |
| ---- | ---- | ---- |
| 浏览器访问超时 | 云服务器安全组没放行 8080 | 控制台添加入方向规则 TCP 8080 |
| 启动报 `UnsupportedClassVersionError` | 用了 Java 8 运行 | 换成 JDK 17：`java -version` 确认 |
| 启动报端口被占用 | 8080 已被其他程序占用 | `server.port` 改端口，或 `pkill -f calculator` |
| 前端打不开历史、请求 404 | `config.js` 里 `API_BASE` 没配或配错 | 改成后端公网地址，注意 `http://` 前缀和端口 |
| 历史记录刷新后没了 | H2 数据目录不持久 | 方案 A：确认在 `/root/calculator/data`；方案 B：挂载持久卷 |
| 容器平台部署后反复重启 | 内存不足（默认 JVM 堆较大） | 设置环境变量 `JAVA_OPTS=-Xmx256m` |
| 页面样式丢失 | 静态资源路径问题 | 确认 `src/main/resources/static/` 下四个文件齐全 |
