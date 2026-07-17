FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -Dmaven.test.skip=true
RUN wget -q https://repo1.maven.org/maven2/io/sentry/sentry-opentelemetry-agent/8.49.0/sentry-opentelemetry-agent-8.49.0.jar -O sentry-opentelemetry-agent.jar

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache tzdata
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
COPY --from=builder /app/sentry-opentelemetry-agent.jar sentry-opentelemetry-agent.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Duser.timezone=America/Sao_Paulo -jar app.jar"]
