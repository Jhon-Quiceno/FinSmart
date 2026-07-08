# Build
FROM eclipse-temurin:21-jdk-alpine AS build
RUN apk update && apk upgrade --no-cache
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw mvnw
RUN chmod +x mvnw
COPY pom.xml pom.xml
RUN ./mvnw --batch-mode --no-transfer-progress dependency:go-offline
COPY src src
RUN ./mvnw --batch-mode --no-transfer-progress package -DskipTests

# Runtime
FROM eclipse-temurin:21-jre-alpine
RUN apk update && apk upgrade --no-cache
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
