FROM gradle:7.4.1-jdk17-alpine as build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR --/hom/gradle/src
RUN gradle bootJar --no-daemon

EXPOSE 80
ENTRYPOINT ["dotnet", "Lott.Api.Retail.dll","--urls", "http://+:80"]