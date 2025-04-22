# Etapa de build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Etapa de runtime cu Chrome/Chromedriver
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Chrome & deps compatibile cu Debian slim / Ubuntu
RUN apt-get update && \
    apt-get install -y wget curl unzip gnupg ca-certificates fonts-liberation libasound2 libatk-bridge2.0-0 libatk1.0-0 libcups2 libdbus-1-3 libgdk-pixbuf2.0-0 libnspr4 libnss3 libxcomposite1 libxrandr2 libxss1 libxtst6 xdg-utils && \
    curl -fsSL https://dl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /usr/share/keyrings/google.gpg && \
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google.gpg] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google.list && \
    apt-get update && \
    apt-get install -y google-chrome-stable

ENV CHROME_DRIVER_VERSION=122.0.6261.94

RUN wget -O /tmp/chromedriver.zip https://edgedl.me.gvt1.com/edgedl/chrome/chrome-for-testing/${CHROME_DRIVER_VERSION}/linux64/chromedriver-linux64.zip && \
    unzip /tmp/chromedriver.zip -d /usr/local/bin/ && \
    mv /usr/local/bin/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver && \
    chmod +x /usr/local/bin/chromedriver && \
    rm -rf /tmp/*

ENV PATH="/usr/local/bin:$PATH"

COPY --from=build /app/target/RecordsScrapers-1.0-SNAPSHOT.jar /app/app.jar
EXPOSE 8000
CMD ["java", "-jar", "app.jar"]
