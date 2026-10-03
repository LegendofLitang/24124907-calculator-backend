# ============================================================
# 计算器系统 · 后端镜像
# 多阶段构建：构建阶段用 Maven + JDK 17，运行阶段只带 JRE，镜像更小
#
# 本地构建并运行：
#   docker build -t calculator-backend .
#   docker run -d -p 8080:8080 -v calculator-data:/app/data --name calc calculator-backend
#
# 说明：H2 是文件型数据库，数据写在容器内的 /app/data 目录。
#       部署到容器平台时**必须把这个目录挂载为持久卷**，
#       否则容器重建后历史记录会丢失。
# ============================================================

# ---------- 阶段一：构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先只复制 pom.xml 并预下载依赖，利用 Docker 层缓存加速后续构建
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 再复制源码并打包（跳过测试，测试在 CI / 本地执行）
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 阶段二：运行 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 复制构建产物
COPY --from=build /build/target/calculator-system-1.0.0.jar app.jar

# H2 数据目录：容器平台请挂载持久卷到该路径
VOLUME ["/app/data"]

# 云平台通常通过 PORT 环境变量指定端口，未设置时回退到 8080
ENV PORT=8080
EXPOSE 8080

# JAVA_OPTS 可用于限制内存，例如 -Xmx256m
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
