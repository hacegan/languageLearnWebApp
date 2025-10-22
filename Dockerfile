# Build stage: Compile with Maven and Java 21
FROM openjdk:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY src src
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

# Runtime stage: Run the JAR
FROM openjdk:21-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
#COPY src/main/resources/firebase-service-account.json firebase-service-account.json
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]