# Build stage: compile the application with Maven (JDK 17)
FROM maven:3.9.0 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage: run the produced jar with a lightweight JRE
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copy the jar produced by the builder stage (assumes a single jar in target)
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]