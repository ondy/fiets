#!/bin/bash
EMAIL="asiemer@gmx.de"
PASS="test"

JAR=$(ls fiets-*.jar | tail -1)
nohup java -Dfeverapi.email="$EMAIL" -Dfeverapi.password="$PASS" -jar $JAR \
  >stdout.txt 2>stderr.txt </dev/null  & echo $! >fiets.pid
