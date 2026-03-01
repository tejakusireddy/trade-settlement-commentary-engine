FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -pl services/api-gateway-service -am

FROM eclipse-temurin:21-jre-alpine
COPY --from=builder /app/services/api-gateway-service/target/*.jar app.jar
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
