FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY . .

RUN mvn clean package

# ==============================

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=build /app/target /app/target
COPY src /app/src
COPY pom.xml /app/pom.xml

EXPOSE 8000

CMD ["java", "-cp", "target/classes", "MainServer"]
