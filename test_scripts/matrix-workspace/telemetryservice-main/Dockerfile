# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-jammy@sha256:ce5767b7222312d42395f5bab033cd91f09e44032a2f21bdfd7b5b912dbe1e77 AS builder

WORKDIR /telemetryservice

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && sed -i 's/\r$//' mvnw
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package

# Stage 2: Create minimal runtime image
FROM eclipse-temurin:21-alpine@sha256:6ea5548706b60ac0a602eaf48af74792cbab012d90e811ca8db6184b16b5c3d6

RUN apk add --no-cache curl
RUN addgroup -S -g 1000 msinit && adduser -S -u 1000 -G msinit msinit

WORKDIR /telemetryservice

COPY --chown=msinit:msinit --from=builder /telemetryservice/target/*.jar telemetryservice.jar

ENV JAVA_OPTS=""

EXPOSE 6050

USER msinit
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar telemetryservice.jar"]
