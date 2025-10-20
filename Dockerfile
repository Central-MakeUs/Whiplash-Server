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

# 로그 디렉토리를 미리 생성하고 소유권을 nuntteo 사용자에게 부여
# 이 작업은 컨테이너 내부에서 로그 파일 생성 시 발생하는 권한 문제를 해결
RUN mkdir -p /app/logs && chown -R nuntteo:nuntteo /app/logs

RUN chown nuntteo:nuntteo app.jar

USER nuntteo

EXPOSE 8080

# 컨테이너 실행 시 환경 변수(SPRING_PROFILES_ACTIVE)로 프로필을 주입
ENTRYPOINT ["java", "-jar", "/app/app.jar"]