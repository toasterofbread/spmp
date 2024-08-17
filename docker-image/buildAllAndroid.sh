#!/bin/sh
set -e

docker-image/dockerBuild.sh androidApp:packageRelease -PenableApkSplit
