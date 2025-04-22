# Etapa de build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa de runtime
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Instalăm Chromium + ChromeDriver + deps (cu libasound2t64)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    chromium-driver chromium \
    fonts-liberation libasound2t64 libatk-bridge2.0-0 libatk1.0-0 \
    libcups2 libdbus-1-3 libgdk-pixbuf2.0-0 libnspr4 libnss3 \
    libxcomposite1 libxrandr2 libxss1 libxtst6 xdg-utils && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

ENV CHROME_BIN=/usr/bin/chromium
ENV CHROMEDRIVER_PATH=/usr/bin/chromedriver
ENV PATH="${CHROMEDRIVER_PATH}:${PATH}"

COPY --from=build /app/target/RecordsScrapers-1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8000
CMD ["java", "-jar", "app.jar"]
