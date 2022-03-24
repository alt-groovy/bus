FROM gradle:7.4.1-jdk11-alpine as build

RUN gradle --version && java -version

# Only copy dependency-related files
COPY --chown=gradle:gradle build.gradle settings.gradle /app/

# Only download dependencies
# Eat the expected build failure since no source code has been copied yet
RUN gradle clean build --no-daemon > /dev/null 2>&1 || true

# Copy all/ remaining files
COPY --chown=gradle:gradle . /home/gradle/src

WORKDIR /home/gradle/src

RUN gradle bootJar --no-daemon

FROM azul/zulu-openjdk-alpine:11.0.12

COPY --from=build /home/gradle/src/build/libs/*.jar /app/

EXPOSE 9091
ENTRYPOINT ["java","-jar","-Dspring.profiles.active=n1","/app/bus-1.0.0.jar"]