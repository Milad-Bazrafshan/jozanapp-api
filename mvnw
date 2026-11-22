#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
maven_home="$project_dir/.mvn/apache-maven-3.9.11"
maven_archive="$project_dir/.mvn/apache-maven-3.9.11-bin.tar.gz"
maven_url="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz"

if [ ! -x "$maven_home/bin/mvn" ]; then
  if [ ! -f "$maven_archive" ]; then
    printf '%s\n' 'Downloading Apache Maven 3.9.11...'
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$maven_url" -o "$maven_archive"
    elif command -v wget >/dev/null 2>&1; then
      wget -q "$maven_url" -O "$maven_archive"
    else
      printf '%s\n' 'curl or wget is required to bootstrap Maven.' >&2
      exit 1
    fi
  fi
  tar -xzf "$maven_archive" -C "$project_dir/.mvn"
fi

exec "$maven_home/bin/mvn" "$@"
