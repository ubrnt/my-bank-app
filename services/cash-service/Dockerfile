FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
COPY libs/bank-chassis/build.gradle libs/bank-chassis/build.gradle
COPY libs/bank-persistence-starter/build.gradle libs/bank-persistence-starter/build.gradle
COPY libs/bank-notifications-outbox-starter/build.gradle libs/bank-notifications-outbox-starter/build.gradle
COPY services/cash-service/build.gradle services/cash-service/build.gradle
RUN --mount=type=cache,target=/root/.gradle,sharing=locked ./gradlew --no-daemon :cash-service:resolveDependencies

COPY libs/ libs/
COPY services/cash-service/src services/cash-service/src
RUN --mount=type=cache,target=/root/.gradle,sharing=locked ./gradlew --no-daemon -x test :cash-service:bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/cash-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
