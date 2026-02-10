# Nanobot Java - Docker 镜像
FROM eclipse-temurin:21-jdk-alpine

# 设置工作目录
WORKDIR /app

# 安装 Maven
RUN apk add --no-cache maven

# 复制项目文件
COPY pom.xml .
COPY src ./src

# 编译项目
RUN mvn clean package -DskipTests

# 创建工作目录
RUN mkdir -p /root/.nanobot/workspace /root/.nanobot/data

# 暴露端口（如果需要 API 服务）
EXPOSE 8080

# 设置环境变量
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# 启动命令
ENTRYPOINT ["java", "-jar", "target/nanobot-1.0.0.jar"]
CMD []
