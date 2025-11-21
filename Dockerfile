# Build stage
FROM gradle:8.10-jdk21 AS build
WORKDIR /src

USER root
RUN chown -R gradle:gradle /src
USER gradle

COPY --chown=gradle:gradle . /src/fiets
WORKDIR /src/fiets
RUN gradle build

# Runtime stage
FROM eclipse-temurin:21-jre

ENV FIETS_HOME /usr/local/fiets
ENV PATH $FIETS_HOME/bin:$PATH
RUN mkdir -p "$FIETS_HOME" \
  && useradd --create-home --shell /bin/false fiets-user \
  && chown fiets-user $FIETS_HOME

USER fiets-user
WORKDIR $FIETS_HOME

COPY --from=build --chown=fiets-user:fiets-user /src/fiets/build/libs/fiets-*.jar fiets.jar

EXPOSE 8080
CMD ["java", "-jar", "fiets.jar"]
