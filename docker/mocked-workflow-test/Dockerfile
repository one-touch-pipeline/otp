# Copyright 2011-2026 The OTP authors
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

# documentation: https://hub.docker.com/r/linuxserver/openssh-server
FROM ghcr.io/linuxserver/openssh-server

# The Groovy Version that we want to install
ARG GROOVY_VERSION=3.0.25

# user home defined by the container
ENV OTP_HOME=/config
ENV WORKFLOWS=/workflows
ENV SDKMAN=$WORKFLOWS/.sdkman
ENV LOGS=$WORKFLOWS/logs
ENV JOBS=$WORKFLOWS/jobs
ENV GROOVY=$WORKFLOWS/groovy
ENV GROOVY_SERVICE=$WORKFLOWS/groovy-service
ENV BIN=$WORKFLOWS/bin
ENV REFERENCE_DATA=$WORKFLOWS/reference-data

RUN apk update && \
    apk add --no-cache \
        curl \
        openjdk17 \
        tree \
        zip && \
    rm -rf /var/cache/apk/*

# create directories needed for mocked workflow tests
RUN mkdir -p $WORKFLOWS/tests $WORKFLOWS/ngs_share $WORKFLOWS/virtualenvs \
    $REFERENCE_DATA $JOBS $LOGS $GROOVY $GROOVY_SERVICE $BIN $OTP_HOME && \
    chown -R 1000:1002 $WORKFLOWS

# needed for sdk install
ENV HOME=$WORKFLOWS

# Install SDKMAN and Groovy
# java installed via sdkman don't work because of missing dependencies, which conflicts with existing installed packages
RUN curl -s "https://get.sdkman.io" | bash && \
    bash -c "source $SDKMAN/bin/sdkman-init.sh && \
    sdk install groovy $GROOVY_VERSION" && \
    chown -R 1000:1002 $SDKMAN

# add init script for ssh server (will execute on starting)
ADD init /custom-cont-init.d/
RUN chown -R root:root /custom-cont-init.d/ && \
    chmod -R u+rwx,go-rwx /custom-cont-init.d/

# add service
ADD groovy-service $GROOVY_SERVICE

# add scripts for mocking commands
ADD groovy $GROOVY
RUN for i in "$GROOVY"/*.groovy; do \
      FILE_NAME="$(basename "$i" .groovy)"; \
      printf '#!/bin/bash\n\nset -e\n\nsource "$HOME/.sdkman/bin/sdkman-init.sh"\n\ngroovy "%s/%s.groovy" "$@"\n' \
        "$GROOVY" "$FILE_NAME" > "$BIN/$FILE_NAME"; \
      chown -R 1000:1002 "$BIN/$FILE_NAME"; \
      chmod +x "$BIN/$FILE_NAME"; \
    done

# add profile, used only on interactive login
ADD .profile $OTP_HOME/

# link scripts in system, so they are available also for non interactive remote shells
RUN ln -s -t /usr/local/bin $BIN/*
