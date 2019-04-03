FROM gradle:5.3
WORKDIR /src

USER root
RUN chown -R gradle:gradle /src
USER gradle

RUN git clone https://github.com/ondy/fiets ;\ 
  cd fiets ;\
  gradle build

FROM adoptopenjdk/openjdk8:alpine-jre

ENV FIETS_HOME /usr/local/fiets
ENV PATH $FIETS_HOME/bin:$PATH
RUN mkdir -p "$FIETS_HOME"; \
  adduser -D -g '' fiets-user; \
  chown fiets-user $FIETS_HOME

USER fiets-user
WORKDIR $FIETS_HOME

COPY --from=0 --chown=fiets-user:fiets-user /src/fiets/build/libs/fiets-*.jar fiets.jar

EXPOSE 7000
CMD ["java", "-jar", "fiets.jar"]