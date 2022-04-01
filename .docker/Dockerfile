FROM gradle/dependency-cached:latest as build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle bootJar --no-daemon

FROM azul/zulu-openjdk-alpine:11.0.12
COPY --from=build /home/gradle/src/build/libs/*.jar /app/
