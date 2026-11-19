# --- Étape 1 : compilation ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q package -DskipTests

# --- Étape 2 : image d'exécution légère ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Profil prod par défaut : jamais de secret JWT par défaut ni de ddl-auto=update dans l'image
ENV SPRING_PROFILES_ACTIVE=prod
# Ne pas exécuter l'API en root
RUN groupadd -r app && useradd -r -g app app
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
