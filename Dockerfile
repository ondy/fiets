FROM adoptopenjdk/openjdk8:alpine-jre

ENV FIETS_HOME /usr/local/fiets
ENV PATH $FIETS_HOME/bin:$PATH
RUN mkdir -p "$FIETS_HOME"; \
  adduser -D -g '' fiets-user; \
  chown fiets-user $FIETS_HOME

USER fiets-user
WORKDIR $FIETS_HOME

ENV FIETS_VERSION 0.10
ENV FIETS_URL https://github.com/ondy/fiets/releases/download/v$FIETS_VERSION/fiets-$FIETS_VERSION.0.jar
ENV FIETS_SHA512 04f58bd98d6fd28e6e3a9f70b765118d0a4441a08a23b1c3c86fbe8fba5e5804b4817f8a451e9e63a03dc3414dc930c515faa016d73ff4b821d4f6dd5cda86d7

RUN wget -O fiets.jar "$FIETS_URL";\
  echo "$FIETS_SHA512 *fiets.jar" | sha512sum -c

EXPOSE 7000
CMD ["java", "-jar", "fiets.jar"]