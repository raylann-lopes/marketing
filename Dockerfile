FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -Dmaven.test.skip=true
RUN wget -q https://repo1.maven.org/maven2/io/sentry/sentry-opentelemetry-agent/8.49.0/sentry-opentelemetry-agent-8.49.0.jar -O sentry-opentelemetry-agent.jar \
    && echo "d8b884b7c0756638f15b22941a980f10d24e2d0c13fce6eab717a7fa0b722a7b  sentry-opentelemetry-agent.jar" | sha256sum -c -

# jre-jammy (glibc) em vez de jre-alpine (musl) — o binário nativo do
# async-profiler (usado pelo sentry-async-profiler) só é compilado pra
# glibc; em musl ele falha ao carregar e o profiling fica mudo no Sentry
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends tzdata \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
COPY --from=builder /app/sentry-opentelemetry-agent.jar sentry-opentelemetry-agent.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Duser.timezone=America/Sao_Paulo -jar app.jar"]
