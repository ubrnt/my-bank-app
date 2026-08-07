FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
COPY services/gateway-service/build.gradle services/gateway-service/build.gradle
RUN ./gradlew --no-daemon :gateway-service:resolveDependencies

COPY services/gateway-service/src services/gateway-service/src
RUN ./gradlew --no-daemon -x test :gateway-service:bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/gateway-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
