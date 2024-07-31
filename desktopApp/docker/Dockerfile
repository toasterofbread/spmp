FROM ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y openjdk-21-jre wget git unzip binutils desktop-file-utils

RUN wget https://download.oracle.com/java/22/latest/jdk-22_linux-x64_bin.deb && \
    apt-get install -y ./jdk-22_linux-x64_bin.deb

RUN wget https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage -O /appimagetool && \
    chmod +x /appimagetool && \
    echo "#!/bin/sh" >> /usr/local/bin/appimagetool && \
    echo '/appimagetool --appimage-extract-and-run "$@"' >> /usr/local/bin/appimagetool && \
    chmod +x /usr/local/bin/appimagetool

ARG GRADLE_VERSION
RUN test -n "$GRADLE_VERSION"

RUN wget https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip -O gradle-bin.zip && \
    unzip gradle-bin.zip

ENV GRADLE_HOME=/gradle-$GRADLE_VERSION
ENV GRADLE_USER_HOME=/gradle-user-home

ENV JAVA_21_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV JAVA_22_HOME=/usr/lib/jvm/jdk-22.0.2-oracle-x64
ENV JAVA_HOME=$JAVA_22_HOME

WORKDIR /src
ENTRYPOINT ["/src/desktopApp/docker/entryPoint.sh"]
