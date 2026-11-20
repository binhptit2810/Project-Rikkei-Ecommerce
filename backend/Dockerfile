# =============================================================================
# Dockerfile — Backend (Spring Boot / Java 21)
# Multi-stage build: giảm kích thước image
# =============================================================================

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Gradle wrapper và cấu hình trước để tận dụng Docker cache
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/

# Tải dependencies (sẽ cache nếu build.gradle không thay đổi)
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src/ src/

# Build ứng dụng (bỏ qua test để build nhanh hơn; test chạy trong CI)
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Thiết lập múi giờ Việt Nam đồng nhất (Asia/Ho_Chi_Minh - UTC+7)
ENV TZ=Asia/Ho_Chi_Minh

# Cài đặt tzdata cho timezone và fontconfig/ttf-dejavu cho OpenPDF/iText kết xuất PDF tiếng Việt (TSK-D16-DHA)
# Tạo user không phải root để chạy ứng dụng và chuẩn bị thư mục uploads
RUN apk add --no-cache tzdata fontconfig ttf-dejavu && \
    cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone && \
    addgroup -g 1001 spring && adduser -u 1001 -G spring -s /bin/sh -D spring && \
    mkdir -p /app/uploads && chown -R spring:spring /app/uploads
USER spring:spring

# Copy JAR từ stage build
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# Chờ database sẵn sàng qua healthcheck của Docker Compose; nạp cấu hình qua biến môi trường runtime
ENTRYPOINT ["sh", "-c", "exec java -Duser.timezone=Asia/Ho_Chi_Minh -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker} -jar app.jar"]
