FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app
COPY . .

# Folosește un JAR executabil cu toate deps incluse
RUN mvn clean package -DskipTests

# ==============================

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar

# Render setează $PORT, aplicația trebuie să-l folosească
EXPOSE 8000

CMD ["java", "-jar", "app.jar"]
