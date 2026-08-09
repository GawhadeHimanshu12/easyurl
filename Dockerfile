# Stage 1: Build Frontend
FROM node:18-alpine AS frontend-builder
WORKDIR /app/client
COPY client/package*.json ./
RUN npm install
COPY client/ ./
RUN npm run build

# Stage 2: Build Backend
FROM maven:3.9-eclipse-temurin-21 AS backend-builder
WORKDIR /app/server
COPY server/pom.xml .
COPY server/.mvn ./.mvn
COPY server/mvnw .
RUN ./mvnw dependency:go-offline -B
COPY server/src ./src
# Copy React build to Spring Boot static resources
COPY --from=frontend-builder /app/client/dist ./src/main/resources/static
RUN ./mvnw clean package -DskipTests

# Stage 3: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/server/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
