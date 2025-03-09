FROM openjdk:17-jdk-slim as build

WORKDIR /app

COPY gradlew ./
COPY gradle/wrapper /app/gradle/wrapper
COPY build.gradle ./
COPY settings.gradle ./

COPY src /app/src

RUN chmod +x ./gradlew

RUN ./gradlew build -x test

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/build/libs/storage-1.0.jar /app/storage-1.0.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/storage-1.0.jar"]