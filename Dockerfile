FROM openjdk:21-jdk-slim

WORKDIR /app

COPY build/libs/server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

# 컨테이너 실행 시 Spring Boot 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]