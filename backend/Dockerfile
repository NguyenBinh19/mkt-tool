FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /build
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

COPY pom.xml .
RUN mvn -B dependency:go-offline
RUN mvn -B exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
COPY src ./src
RUN mvn -B verify

FROM eclipse-temurin:21-jre-jammy

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       ca-certificates libasound2 libatk-bridge2.0-0 libatk1.0-0 libcups2 \
       libdrm2 libgbm1 libgtk-3-0 libnspr4 libnss3 libu2f-udev libx11-6 \
       libx11-xcb1 libxcb1 libxcomposite1 libxdamage1 libxfixes3 \
       libxkbcommon0 libxrandr2 fonts-liberation \
       wget xvfb x11vnc novnc websockify \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar
COPY --from=build /ms-playwright /ms-playwright
COPY docker/start.sh /app/start.sh
RUN mkdir -p /app/playwright-profile

ENV DISPLAY=:99
EXPOSE 8080 6080
ENTRYPOINT ["bash", "/app/start.sh"]
