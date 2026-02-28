# 1. Build Stage: 빌드 시에도 Alpine 사용 (Mac 로컬 호환성을 위해 JDK 버전 사용)
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Gradle Wrapper 및 의존성 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon

# 2. Runtime Stage: 최경량화를 위해 JRE Alpine 사용
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# 보안을 위해 비루트 사용자 생성
RUN addgroup -g 1000 nuntteo && \
    adduser -u 1000 -G nuntteo -h /home/nuntteo -D nuntteo

# 빌드된 JAR 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 로그 디렉토리 생성 및 권한 부여
RUN mkdir -p /app/logs && \
    chown -R nuntteo:nuntteo /app/logs && \
    chmod 755 /app/logs && \
    chmod 444 app.jar

# 사용자 전환
USER nuntteo
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]