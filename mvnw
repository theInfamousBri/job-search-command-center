#!/bin/sh
# Lightweight Maven Wrapper launcher for this project.
# Maven distribution is pinned in .mvn/wrapper/maven-wrapper.properties.
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$PROPERTIES" ]; then
  echo "Error: $PROPERTIES is missing." >&2
  exit 1
fi

distributionUrl=$(sed -n 's/^distributionUrl=//p' "$PROPERTIES" | tail -n 1)
if [ -z "$distributionUrl" ]; then
  echo "Error: distributionUrl is not configured in $PROPERTIES." >&2
  exit 1
fi

archiveName=${distributionUrl##*/}
mavenVersion=${archiveName#apache-maven-}
mavenVersion=${mavenVersion%-bin.zip}

MAVEN_USER_HOME=${MAVEN_USER_HOME:-"$HOME/.m2"}
wrapperHome="$MAVEN_USER_HOME/wrapper/dists/apache-maven-$mavenVersion"
mavenHome="$wrapperHome/apache-maven-$mavenVersion"
mavenBin="$mavenHome/bin/mvn"

if [ ! -x "$mavenBin" ]; then
  mkdir -p "$wrapperHome"
  tmpDir=$(mktemp -d 2>/dev/null || mktemp -d -t mvnw)
  archive="$tmpDir/$archiveName"
  trap 'rm -rf "$tmpDir"' EXIT HUP INT TERM

  echo "Downloading Apache Maven $mavenVersion..." >&2
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --silent --show-error "$distributionUrl" --output "$archive"
  elif command -v wget >/dev/null 2>&1; then
    wget --quiet "$distributionUrl" --output-document="$archive"
  else
    echo "Error: curl or wget is required for the first Maven Wrapper run." >&2
    exit 1
  fi

  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$archive" -d "$wrapperHome"
  elif command -v jar >/dev/null 2>&1; then
    (cd "$wrapperHome" && jar xf "$archive")
  else
    echo "Error: unzip or the JDK 'jar' tool is required to unpack Maven." >&2
    exit 1
  fi

  rm -rf "$tmpDir"
  trap - EXIT HUP INT TERM
fi

if [ ! -x "$mavenBin" ]; then
  echo "Error: Maven Wrapper could not install Maven $mavenVersion." >&2
  exit 1
fi

exec "$mavenBin" "$@"
