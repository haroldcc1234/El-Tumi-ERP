FROM node:22-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /build/backend
COPY backend/pom.xml ./
RUN mvn -B dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /build/frontend/dist/ ./src/main/resources/static/
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system tumi && useradd --system --gid tumi --home-dir /app tumi \
    && mkdir -p /app/private-files && chown -R tumi:tumi /app
COPY --from=backend /build/backend/target/el-tumi-erp-1.0.0.jar /app/el-tumi-erp.jar
USER tumi
ENV BIND_ADDRESS=0.0.0.0
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=America/Lima", "-jar", "/app/el-tumi-erp.jar"]
