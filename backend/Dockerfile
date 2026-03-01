# --- Build stage ---
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace

# Copy Gradle wrapper and build config first for dependency caching
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew dependencies --no-daemon -q

# Copy source and build (layer invalidated only when source changes)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# --- Custom JRE stage: jlink strips unused JRE modules ---
FROM eclipse-temurin:25-jdk-alpine AS jre-build
COPY --from=build /workspace/build/libs/PDF-Master-0.0.1-SNAPSHOT.jar /tmp/app.jar
RUN mkdir -p /tmp/app && cd /tmp/app && jar xf /tmp/app.jar && \
    DEPS=$(jdeps \
      --ignore-missing-deps \
      --print-module-deps \
      --multi-release 25 \
      --class-path 'BOOT-INF/lib/*' \
      BOOT-INF/classes 2>/dev/null || echo "java.base") && \
    jlink \
      --add-modules "${DEPS},jdk.crypto.ec" \
      --strip-debug \
      --compress=zip-9 \
      --no-header-files \
      --no-man-pages \
      --output /opt/jre

# --- Runtime stage: plain Alpine + custom JRE ---
FROM alpine:3.21

# Native libs for JRE + fonts for PDF processing
RUN apk add --no-cache fontconfig ttf-dejavu libstdc++ && \
    addgroup -S appuser && adduser -S -G appuser -h /app -s /sbin/nologin appuser

ENV JAVA_HOME=/opt/jre
ENV PATH="$JAVA_HOME/bin:$PATH"

WORKDIR /app

COPY --from=jre-build /opt/jre $JAVA_HOME
COPY --from=build --chown=appuser:appuser \
    /workspace/build/libs/PDF-Master-0.0.1-SNAPSHOT.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
