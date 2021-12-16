FROM openjdk:11.0.12-slim

# hadolint ignore=DL3008
RUN apt-get update \
 && apt-get -y install --no-install-recommends curl unzip \
 && apt-get clean \
 && rm -rf /var/lib/apt/lists/*

ENV ANDROID_HOME /opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/bin

ARG COMMAND_LINE_TOOLS=commandlinetools-linux-7583922_latest

RUN mkdir -p $ANDROID_HOME \
 && curl -o /tmp/${COMMAND_LINE_TOOLS}.zip https://dl.google.com/android/repository/${COMMAND_LINE_TOOLS}.zip \
 && unzip -d ${ANDROID_HOME} /tmp/${COMMAND_LINE_TOOLS}.zip \
 && rm /tmp/${COMMAND_LINE_TOOLS}.zip

RUN mkdir ~/.android \
 && touch ~/.android/repositories.cfg

ARG BUILD_TOOLS_VERSION=31.0.0
ARG PLATFORMS_VERSION=android-31

# hadolint ignore=DL4006
RUN yes | sdkmanager --sdk_root=${ANDROID_HOME} --licenses
RUN sdkmanager --sdk_root=${ANDROID_HOME} "build-tools;${BUILD_TOOLS_VERSION}" \
 && sdkmanager --sdk_root=${ANDROID_HOME} "platforms;${PLATFORMS_VERSION}" "platform-tools"
