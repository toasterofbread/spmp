#!/bin/sh
set -e

docker-image/dockerBuild.sh desktopApp:packageReleaseUberJarForCurrentOS desktopApp:packageReleaseAppImage desktopApp:packageReleaseTarball
