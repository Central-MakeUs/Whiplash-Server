# Build Stage
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Gradle Wrapper 및 의존성 관련 파일 먼저 복사 (캐싱 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 빌드
RUN ./gradlew bootJar --no-daemon

# Runtime Stage
FROM eclipse-temurin:17-jre-alpine AS runtime

# 비루트 사용자 생성 (보안)
RUN addgroup -g 1000 nuntteo && adduser -u 1000 -G nuntteo -s /bin/sh -D nuntteo

WORKDIR /app

# 빌드된 JAR 복사
COPY --from=build /app/build/libs/*.jar app.jar

RUN chown nuntteo:nuntteo app.jar

USER nuntteo

EXPOSE 8080

# dev 프로필로 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
