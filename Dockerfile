FROM gradle:8.10.2-jdk21 AS build
WORKDIR /src

USER root
RUN chown -R gradle:gradle /src
USER gradle

COPY --chown=gradle:gradle . /src/fiets
WORKDIR /src/fiets
RUN gradle build

FROM eclipse-temurin:21-jre-alpine

ENV FIETS_HOME /usr/local/fiets
ENV PATH $FIETS_HOME/bin:$PATH
RUN mkdir -p "$FIETS_HOME"; \
  adduser -D -g '' fiets-user; \
  chown fiets-user $FIETS_HOME

USER fiets-user
WORKDIR $FIETS_HOME

COPY --from=build --chown=fiets-user:fiets-user /src/fiets/build/libs/fiets-*.jar fiets.jar

EXPOSE 7000
CMD ["java", "-jar", "fiets.jar"]
