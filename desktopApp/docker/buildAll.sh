set -e

./gradlew --stop
pushd desktopApp/docker
./dockerBuild.sh packageReleaseUberJarForCurrentOS packageReleaseAppImage packageReleaseTarball -PGIT_TAG_OVERRIDE=$TAG
popd
