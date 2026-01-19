# backend/Dockerfile

# === Build Stage ===
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Gradle 캐시 활용을 위해 의존성 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 레이어)
RUN gradle dependencies --no-daemon || true

# 소스 복사 및 빌드
COPY src ./src
RUN gradle bootJar --no-daemon

# === Runtime Stage ===
FROM eclipse-temurin:21-jre

WORKDIR /app

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]