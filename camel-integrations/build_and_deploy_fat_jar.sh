#!/bin/sh
./gradlew build -Dquarkus.package.jar.type=uber-jar -x test

VERSION=0.0.5 # adjust src/main/manifests/deployment-fatjar.yaml as well

docker build --platform linux/amd64 -t quay.io/rh_ee_mvandepe/cdc-based-integration-example-camel:$VERSION-fatjar \
        -f /src/main/container/ContainerImageDefinition \
        .
docker push quay.io/rh_ee_mvandepe/cdc-based-integration-example-camel:$VERSION-fatjar

oc apply -f src/main/manifests/deployment-fatjar.yaml
