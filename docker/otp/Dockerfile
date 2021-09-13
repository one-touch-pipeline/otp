# Copyright 2011-2020 The OTP authors
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

FROM registry.gitlab.com/one-touch-pipeline/otp/otp-gitlab-ci:latest

ARG APP_HOME=/home/otp/app
RUN mkdir -p $GRADLE_USER_HOME $APP_HOME
ARG GRADLE_PROXY_ENABLED=false
ARG GRADLE_PROXY_HOST
ARG GRADLE_PROXY_PORT

ENV GRADLE_COMMAND=bootRun
ENV OTP_PROPERTIES=$APP_HOME/.otp.properties

# example data for meta data import
ADD --chown=otp https://gitlab.com/one-touch-pipeline/otp/-/wikis/uploads/c6ced70036c6f2e46b54c1ea589a7763/otp_example_data.zip /home/otp
RUN unzip -q /home/otp/otp_example_data.zip && rm /home/otp/otp_example_data.zip

WORKDIR $APP_HOME

COPY . .

RUN if [ "$GRADLE_PROXY_ENABLED" = "true" ] ; then \
    echo "systemProp.http.proxyHost=${GRADLE_PROXY_HOST}" >> ./gradle.properties && \
    echo "systemProp.http.proxyPort=${GRADLE_PROXY_PORT}" >> ./gradle.properties && \
    echo "systemProp.https.proxyHost=${GRADLE_PROXY_HOST}" >> ./gradle.properties && \
    echo "systemProp.https.proxyPort=${GRADLE_PROXY_PORT}" >> ./gradle.properties \
    ; fi

ENTRYPOINT ./gradlew $GRADLE_COMMAND --stacktrace

EXPOSE 8080