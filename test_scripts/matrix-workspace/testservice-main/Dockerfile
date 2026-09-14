FROM mcr.microsoft.com/playwright/java:v1.46.0-jammy@sha256:66e5d8deb31f6af01393dae3de035f969183beadd7ead93290c40878b2538672

WORKDIR /testservice

COPY src ./src
COPY pom.xml ./

RUN apt-get update && apt-get install -y curl netcat

ENTRYPOINT ["mvn", "clean", "test"]
