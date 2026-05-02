#!/bin/sh
# Gradle wrapper shell script for Linux/macOS

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

MAX_FD="maximum"
warn() { echo "$*"; }
die() { echo; echo "ERROR: $*"; echo; exit 1; }

OS_NAME=$(uname -s | tr '[:upper:]' '[:lower:]')
case $OS_NAME in
  cygwin*|msys*|mingw*) cygwin=true;;
  *) cygwin=false;;
esac

JAVA_EXE=java
if [ -n "$JAVA_HOME" ]; then
  JAVA_EXE="$JAVA_HOME/bin/java"
fi

exec "$JAVA_EXE" -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain "$@"
