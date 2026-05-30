# ===== 第一阶段：构建前端 =====
FROM node:20-alpine AS build

WORKDIR /app

# 复制 package.json 和 package-lock.json
COPY frontend/package*.json ./

# 安装所有依赖（包括devDependencies，因为需要vite构建）
RUN npm install && npm cache clean --force

# 复制源码
COPY frontend/ ./

# 构建生产版本
RUN npm run build

# ===== 第二阶段：Nginx 运行 =====
FROM nginx:1.25-alpine

# 复制 nginx 配置
COPY docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=build /app/dist /usr/share/nginx/html

# 暴露前端端口
EXPOSE 80

# 启动 nginx
CMD ["nginx", "-g", "daemon off;"]