# Etapa de build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa de runtime
FROM eclipse-temurin:17-jdk

WORKDIR /app

# 🔧 Instalează deps și Chrome for Testing + Chromedriver 122
ENV CHROME_VERSION=122.0.6261.94

RUN apt-get update && apt-get install -y wget unzip curl gnupg ca-certificates \
    fonts-liberation libasound2 libatk-bridge2.0-0 libatk1.0-0 libcups2 libdbus-1-3 \
    libgdk-pixbuf2.0-0 libnspr4 libnss3 libxcomposite1 libxrandr2 libxss1 libxtst6 \
    xdg-utils --no-install-recommends && \
    wget -q https://edgedl.me.gvt1.com/edgedl/chrome/chrome-for-testing/${CHROME_VERSION}/linux64/chrome-linux64.zip && \
    wget -q https://edgedl.me.gvt1.com/edgedl/chrome/chrome-for-testing/${CHROME_VERSION}/linux64/chromedriver-linux64.zip && \
    unzip chrome-linux64.zip && mv chrome-linux64 /opt/chrome && \
    unzip chromedriver-linux64.zip && mv chromedriver-linux64/chromedriver /usr/local/bin/chromedriver && \
    chmod +x /usr/local/bin/chromedriver && \
    ln -s /opt/chrome/chrome /usr/local/bin/chrome && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/*

# ✅ Setează environment variables corecte
ENV CHROME_BIN=/usr/local/bin/chrome
ENV CHROMEDRIVER_PATH=/usr/local/bin/chromedriver
ENV PATH="${CHROMEDRIVER_PATH}:${PATH}"

COPY --from=build /app/target/RecordsScrapers-1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8000
CMD ["java", "-jar", "app.jar"]
