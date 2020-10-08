FROM frolvlad/alpine-glibc:alpine-3.10

## install package
RUN apk update && \
    apk upgrade && \
    apk --no-cache --virtual .build add \
    bash \
    openjdk8 \
    wget 

## ENV settings
ENV JAVA_HOME /usr/lib/jvm/java-1.8-openjdk
# See https://developer.android.com/studio#command-tools
ARG SDK_TOOL_VERSION=commandlinetools-linux-6514223_latest
ENV ANDROID_HOME /usr/local/android-sdk-linux
# See https://developer.android.com/studio/releases/build-tools
ARG BUILD_TOOLS_VERSION=29.0.3
ARG PLATFORMS_VERSION=android-29
ENV PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin

# install android sdk tools
RUN mkdir $ANDROID_HOME && \
  wget "https://dl.google.com/android/repository/${SDK_TOOL_VERSION}.zip" && \
  unzip -d $ANDROID_HOME $SDK_TOOL_VERSION.zip && \
  rm -rf $SDK_TOOL_VERSION.zip

# agree sdkmanager licenses
RUN mkdir ~/.android && \
    touch ~/.android/repositories.cfg
#
# Workaround to avoid a bug in sdkmanager
# https://stackoverflow.com/questions/60440509/android-command-line-tools-sdkmanager-always-shows-warning-could-not-creae-se
#
#RUN yes | sdkmanager --licenses
RUN yes | sdkmanager --sdk_root=${ANDROID_HOME} --licenses

# install android tools and more
#RUN sdkmanager "tools" "build-tools;${BUILD_TOOLS_VERSION}" "platforms;${PLATFORMS_VERSION}" "platform-tools" "extras;android;m2repository"
RUN sdkmanager --sdk_root=${ANDROID_HOME} "tools" "build-tools;${BUILD_TOOLS_VERSION}" "platforms;${PLATFORMS_VERSION}" "platform-tools" "extras;android;m2repository"
