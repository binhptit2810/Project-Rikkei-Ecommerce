# =============================================================================
# Dockerfile — Frontend (React + Vite)
# Multi-stage build: Node.js build → Nginx serve
# =============================================================================

# Stage 1: Build React app
FROM node:20-alpine AS builder
WORKDIR /app

# Build argument cho API URL
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

# Tải dependencies trước để cache layer
COPY package*.json ./
RUN npm ci --silent || npm install --no-audit --no-fund

# Copy source và build
COPY . .
RUN npm run build

# Stage 2: Nginx serve static files
FROM nginx:1.27-alpine AS runtime

# Xóa cấu hình mặc định
RUN rm /etc/nginx/conf.d/default.conf

# Copy cấu hình Nginx tùy chỉnh
COPY nginx.conf /etc/nginx/conf.d/app.conf

# Copy file build từ stage trước
COPY --from=builder /app/dist /usr/share/nginx/html

# Gán quyền thư mục tĩnh
RUN chown -R nginx:nginx /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
