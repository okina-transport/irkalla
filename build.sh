#!/usr/bin/env bash

mvn clean install -DskipTests

MVN_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout | sed -r "s/\x1B\[([0-9]{1,3}(;[0-9]{1,2};?)?)?[mGK]//g")

docker build --no-cache -t registry.okina.fr/mobiiti/irkalla:"${MVN_VERSION}" --build-arg JAR_FILE=target/irkalla-"${MVN_VERSION}".jar .
docker push registry.okina.fr/mobiiti/irkalla:"${MVN_VERSION}"