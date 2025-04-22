FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN ./mvnw clean package

EXPOSE 8000

CMD ["java", "-cp", "target/classes", "MainServer"]
