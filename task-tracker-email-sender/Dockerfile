FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY . .

RUN ./gradlew :task-tracker-email-sender:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/task-tracker-email-sender/build/libs/task-tracker-email-sender*.jar ./task-tracker-email-sender.jar

ENTRYPOINT ["java", "-jar", "task-tracker-email-sender.jar"]