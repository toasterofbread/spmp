set -e

PROJECT=$(pwd)/../../
GRADLE_VERSION=$(grep -oP '(?<=gradle-)\d+(\.\d+)+(?=-bin\.zip)' $PROJECT/gradle/wrapper/gradle-wrapper.properties)

docker build -t spmp-build --build-arg="GRADLE_VERSION=$GRADLE_VERSION" .
docker run --rm -it -v $PROJECT:/src -v ~/.gradle:/gradle-user-home spmp-build:latest "$@"
