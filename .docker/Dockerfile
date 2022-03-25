FROM gradle/dependency-cached:latest as build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle bootJar --no-daemon

FROM azul/zulu-openjdk-alpine:11.0.12
COPY --from=build /home/gradle/src/build/libs/*.jar /app/
EXPOSE 9091
ENTRYPOINT ["java","-jar","-Dspring.profiles.active=n1","/app/bus-1.0.0.jar"]