# ---- build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src

# Copy the pom on its own first so the dependency layer is cached until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

RUN groupadd -r paylink && useradd -r -g paylink paylink
COPY --from=build /src/target/*.jar app.jar
USER paylink

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]