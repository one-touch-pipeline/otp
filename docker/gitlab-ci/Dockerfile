# Copyright 2011-2025 The OTP authors
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

FROM ubuntu:24.04
LABEL maintainer="OTP Developers <otp-devel@dkfz-heidelberg.de>"

# The Java Version that we want to install
ARG JAVA_VERSION=17.0.13-tem

# Update package lists and install dependencies
RUN ln -fs /usr/share/zoneinfo/UTC /etc/localtime && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
        alsa-utils \
        git \
        libgtk2.0-dev \
        libgtk-3-dev \
        jq \
        libnotify-bin \
        libxss1 \
        libnss3 \
        postgresql \
        python3-mako \
        python3 \
        python3-venv \
        python3-pip \
        unzip \
        zip \
        xvfb \
        apt-transport-https \
        ca-certificates \
        curl \
        gnupg \
        coreutils \
        lsb-release \
        openssl && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && \
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && \
    apt-get install -y --no-install-recommends docker-ce docker-ce-cli containerd.io docker-compose-plugin && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Configure system via environment variables
ENV GRADLE_USER_HOME=/home/otp/.gradle

# Create and configure user `otp`
RUN useradd -u 911 --system --create-home --shell /bin/bash otp && \
    groupadd othergroup && \
    usermod --append --groups othergroup otp

USER otp
WORKDIR /home/otp

# Install SDKMAN and Java
RUN curl -s "https://get.sdkman.io" | bash && \
    bash -c "source /home/otp/.sdkman/bin/sdkman-init.sh && sdk install java $JAVA_VERSION"

# Set Java environment variables since they are not set automatically by SDKMAN in non-interactive mode
# This does also not work when the bashrc is sourced
ENV JAVA_HOME=/home/otp/.sdkman/candidates/java/current
ENV PATH="$JAVA_HOME/bin:$PATH"

# Fix Cypress output
ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8

# OTP needs the USER environment variable to be defined.
ENV USER=otp

# fix git dubious ownership error
RUN git config --global --add safe.directory /builds/one-touch-pipeline/otp