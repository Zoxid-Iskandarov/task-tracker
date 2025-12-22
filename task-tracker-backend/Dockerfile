FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY . .

RUN ./gradlew :task-tracker-backend:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/task-tracker-backend/build/libs/task-tracker-backend*.jar ./task-tracker-backend.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "task-tracker-backend.jar"]