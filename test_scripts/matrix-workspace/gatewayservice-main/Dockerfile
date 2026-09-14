# Stage 1: Build the application
FROM eclipse-temurin:21-alpine@sha256:6ea5548706b60ac0a602eaf48af74792cbab012d90e811ca8db6184b16b5c3d6 AS builder

WORKDIR /gatewayservice

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

WORKDIR /gatewayservice

COPY --chown=msinit:msinit --from=builder /gatewayservice/target/*.jar gatewayservice.jar
# COPY certs ./certs # Uncomment for production deployment

ENV JAVA_OPTS=""

# EXPOSE 8443 # Uncomment for production deployment
EXPOSE 8080

USER msinit
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar gatewayservice.jar"]
