FROM openjdk:17-jdk-slim

WORKDIR /app

COPY build/libs/storage-1.0.jar /app/storage-1.0.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/storage-1.0.jar"]