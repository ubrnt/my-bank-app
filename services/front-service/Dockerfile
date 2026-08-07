FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
COPY services/front-service/build.gradle services/front-service/build.gradle
RUN ./gradlew --no-daemon :front-service:resolveDependencies

COPY services/front-service/src services/front-service/src
RUN ./gradlew --no-daemon -x test :front-service:bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/services/front-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
