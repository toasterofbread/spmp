FROM ubuntu:20.04

ENV DEBIAN_FRONTEND=noninteractive

ARG GRADLE_VERSION
RUN test -n "$GRADLE_VERSION"

ARG ANDROID_SDK_VERSION
RUN test -n "$ANDROID_SDK_VERSION"

RUN apt-get update && \
    apt-get install -y openjdk-21-jre wget git unzip binutils desktop-file-utils

RUN wget https://download.oracle.com/java/23/latest/jdk-23_linux-x64_bin.deb && \
    apt-get install -y ./jdk-23_linux-x64_bin.deb

RUN wget https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage -O /appimagetool && \
    chmod +x /appimagetool && \
    echo "#!/bin/sh" >> /usr/local/bin/appimagetool && \
    echo '/appimagetool --appimage-extract-and-run "$@"' >> /usr/local/bin/appimagetool && \
    chmod +x /usr/local/bin/appimagetool

ENV ANDROID_HOME=/android-sdk
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdlinetools.zip && \
    unzip cmdlinetools.zip && \
    mkdir -p $ANDROID_HOME/cmdline-tools && \
    mv cmdline-tools $ANDROID_HOME/cmdline-tools/latest

RUN yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses && \
    $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "build-tools;$ANDROID_SDK_VERSION.0.0" "platforms;android-$ANDROID_SDK_VERSION"

RUN wget https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip -O gradle-bin.zip && \
    unzip gradle-bin.zip

ENV GRADLE_HOME=/gradle-$GRADLE_VERSION
ENV GRADLE_USER_HOME=/gradle-user-home

ENV JAVA_21_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV JAVA_23_HOME=/usr/lib/jvm/jdk-23.0.2-oracle-x64
ENV JAVA_HOME=$JAVA_23_HOME

WORKDIR /src
ENTRYPOINT ["/src/docker-image/gradleEntryPoint.sh"]
