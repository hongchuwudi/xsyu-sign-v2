FROM node:22-alpine AS frontend-build

WORKDIR /workspace/xsyu-sign-web

COPY xsyu-sign-web/package.json xsyu-sign-web/package-lock.json ./
RUN npm ci

COPY xsyu-sign-web/ ./
RUN npm run build


FROM maven:3.9.11-eclipse-temurin-17 AS backend-build

WORKDIR /workspace

COPY xsyu-sign-server/pom.xml ./xsyu-sign-server/pom.xml
RUN mvn -B -f xsyu-sign-server/pom.xml dependency:go-offline

COPY xsyu-sign-server/ ./xsyu-sign-server/
COPY --from=frontend-build /workspace/xsyu-sign-web/dist/ ./xsyu-sign-server/src/main/resources/static/

RUN mvn -B -f xsyu-sign-server/pom.xml package -DskipTests


FROM eclipse-temurin:17-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends tzdata \
    && rm -rf /var/lib/apt/lists/*

ENV TZ=Asia/Shanghai

WORKDIR /app

COPY --from=backend-build /workspace/xsyu-sign-server/target/qq-robot-sign-1.1.0.jar /app/app.jar

EXPOSE 11451

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
