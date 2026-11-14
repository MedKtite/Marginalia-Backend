# Readsake backend — production image (built by .github/workflows/deploy.yml)

# ── Build ──
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src

# Dependencies first so they stay cached until pom.xml changes
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar /app.jar

# ── Run ──
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 --create-home app
USER app
WORKDIR /app
COPY --from=build /app.jar app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
