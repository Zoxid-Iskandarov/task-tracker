FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY . .

RUN ./gradlew :task-tracker-scheduler:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/task-tracker-scheduler/build/libs/task-tracker-scheduler*.jar ./task-tracker-scheduler.jar

ENTRYPOINT ["java", "-jar", "task-tracker-scheduler.jar"]