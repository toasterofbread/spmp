#!/bin/sh
set -e

IMAGE="spmp-build"
DOCKER_ARGS="$@"
PROJECT=$(pwd)

GRADLE_VERSION=$(grep -oP '(?<=gradle-)\d+(\.\d+)+(?=-bin\.zip)' $PROJECT/gradle/wrapper/gradle-wrapper.properties)
ANDROID_SDK_VERSION=$(grep "^android.compileSdk=" $PROJECT/gradle.properties | cut -d'=' -f2)

docker build -t $IMAGE --build-arg="GRADLE_VERSION=$GRADLE_VERSION" --build-arg="ANDROID_SDK_VERSION=$ANDROID_SDK_VERSION" docker-image

./gradlew clean
./gradlew --stop
docker run --rm -it -v $PROJECT:/src -v ~/.gradle:/gradle-user-home $IMAGE:latest $DOCKER_ARGS -PGIT_TAG_OVERRIDE=$TAG
