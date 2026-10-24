FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -ntp -DskipTests package
FROM eclipse-temurin:25-jre
RUN groupadd -r bankflow && useradd -r -g bankflow bankflow
WORKDIR /app
COPY --from=builder /build/target/bankflow-ai-2.0.0.jar /app/app.jar
USER bankflow
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
