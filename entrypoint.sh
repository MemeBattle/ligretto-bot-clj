#!/bin/bash

APP_NAME=ligretto-clj-bot
JAR_PATH=/service/app.jar
ENVIRONMENT=prod

# Sets initial values of variables
PWD="$(dirname "$0")"
LOG_DIR="$PWD/log"

export MALLOC_ARENA_MAX=4
# Stop the JVM from being allowed to use up all of
# Docker's virtual memory. Use if it's a problem
# see https://siddhesh.in/posts/malloc-per-thread-arenas-in-glibc.html

set -eu

trap 'error_handler' ERR 1 2 3 4 5 6

error_handler() {
  ERROR_CODE=$?
  echo "App crashed: $ERROR_CODE"
  exit $ERROR_CODE
}

cd $PWD

# JMX prometheus exporter javaagent configuration
JMX_OPTS="-javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent-0.16.1.jar=8080:/opt/jmx_exporter/config.yaml"

# JVM options
JAVA_OPTS="-XX:InitialRAMPercentage=30 -XX:MaxRAMPercentage=85 -XX:+UseContainerSupport -XshowSettings:system "

java $JMX_OPTS \
  $JAVA_OPTS \
  -jar $JAR_PATH "$@"
