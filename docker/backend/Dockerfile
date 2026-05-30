# ===== 第一阶段：构建后端 =====
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# 复制 pom.xml 下载依赖
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码（包含 application-docker.yml）
COPY backend/src ./src

# 编译打包
RUN mvn clean package -DskipTests -B

# ===== 第二阶段：运行后端 =====
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 从构建阶段复制 JAR 包（已包含 application-docker.yml 在 classpath 中）
COPY --from=build /app/target/*.jar app.jar

# 暴露后端端口
EXPOSE 8080

# 使用 docker profile 启动（数据库连接指向 docker-compose 中的 mysql 服务）
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]