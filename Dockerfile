FROM adoptopenjdk/openjdk8:alpine

ENV FIETS_HOME /usr/local/fiets
ENV PATH $FIETS_HOME/bin:$PATH
RUN mkdir -p "$FIETS_HOME"; \
  adduser -D -g '' fiets-user; \
  chown fiets-user $FIETS_HOME

USER fiets-user
WORKDIR $FIETS_HOME

ENV FIETS_VERSION 0.9
ENV FIETS_URL https://github.com/ondy/fiets/releases/download/v$FIETS_VERSION/fiets-$FIETS_VERSION.jar
ENV FIETS_SHA512 2ad7bd3585b90090b81e3293ab20c3aac6c89ec6d2e2eb0f0a150ac1352dab448237d1ed5e8b5539107c03a783cf64ac7f29c4b2000605629f797f8c959d2649

RUN wget -O fiets.jar "$FIETS_URL";\
  echo "$FIETS_SHA512 *fiets.jar" | sha512sum -c

EXPOSE 7000
CMD ["java", "-jar", "fiets.jar"]