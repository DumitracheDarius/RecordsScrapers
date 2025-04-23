# Etapa de build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa de runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

# 1️⃣ Update + deps esențiale
RUN apt-get update && apt-get install -y wget unzip curl gnupg ca-certificates

# 2️⃣ Deps pentru Chrome
RUN apt-get install -y --no-install-recommends \
    fonts-liberation \
    libatk-bridge2.0-0 \
    libatk1.0-0 \
    libcups2 \
    libdbus-1-3 \
    libgdk-pixbuf2.0-0 \
    libnspr4 \
    libnss3 \
    libxcomposite1 \
    libxrandr2 \
    libxss1 \
    libxtst6 \
    libpulse0 \
    libasound2t64 \
    xdg-utils

# 3️⃣ Download Chrome + Chromedriver (versiunea 135.0.7049.97)
RUN wget -q https://storage.googleapis.com/chrome-for-testing-public/135.0.7049.97/linux64/chrome-linux64.zip && \
    unzip chrome-linux64.zip && mv chrome-linux64 /opt/chrome

RUN wget -q https://storage.googleapis.com/chrome-for-testing-public/135.0.7049.97/linux64/chromedriver-linux64.zip && \
    unzip chromedriver-linux64.zip && mv chromedriver-linux64/chromedriver /usr/local/bin/chromedriver && \
    chmod +x /usr/local/bin/chromedriver

# 4️⃣ Link binar Chrome în PATH
RUN ln -s /opt/chrome/chrome /usr/local/bin/chrome

# 5️⃣ Cleanup
RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/*

# 6️⃣ ENV-uri pentru Selenium
ENV CHROME_BIN=/usr/local/bin/chrome
ENV CHROMEDRIVER_PATH=/usr/local/bin/chromedriver
ENV PATH="${CHROMEDRIVER_PATH}:${PATH}"

# 7️⃣ Copiază aplicația
COPY --from=build /app/target/RecordsScrapers-1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8000
CMD ["java", "-jar", "app.jar"]
