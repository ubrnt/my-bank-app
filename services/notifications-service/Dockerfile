FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
COPY libs/bank-chassis/build.gradle libs/bank-chassis/build.gradle
COPY libs/bank-persistence-starter/build.gradle libs/bank-persistence-starter/build.gradle
COPY libs/bank-notifications-outbox-starter/build.gradle libs/bank-notifications-outbox-starter/build.gradle
COPY services/notifications-service/build.gradle services/notifications-service/build.gradle
RUN ./gradlew --no-daemon :notifications-service:resolveDependencies

COPY libs/ libs/
COPY services/notifications-service/src services/notifications-service/src
RUN ./gradlew --no-daemon -x test :notifications-service:bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/notifications-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
