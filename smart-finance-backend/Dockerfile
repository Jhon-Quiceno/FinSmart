# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Se asume que el JAR ya fue generado manualmente en la carpeta target
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
