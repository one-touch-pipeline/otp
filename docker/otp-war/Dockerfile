# Copyright 2011-2021 The OTP authors
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

# This Docker image takes a prebuilt war file and deploys it on a production like tomcat server.
# It is helpful to test scenarios in a prod-like way. Hot reloading is not available.

FROM registry.gitlab.com/one-touch-pipeline/otp/otp-ci-java-17:2023-06-23 as builder

ARG GRADLE_PROXY_ENABLED=false
ARG GRADLE_PROXY_HOST
ARG GRADLE_PROXY_PORT

COPY --chown=otp . .

RUN if [ "$GRADLE_PROXY_ENABLED" = "true" ] ; then \
    echo "systemProp.http.proxyHost=${GRADLE_PROXY_HOST}" >> ./gradle.properties && \
    echo "systemProp.http.proxyPort=${GRADLE_PROXY_PORT}" >> ./gradle.properties && \
    echo "systemProp.https.proxyHost=${GRADLE_PROXY_HOST}" >> ./gradle.properties && \
    echo "systemProp.https.proxyPort=${GRADLE_PROXY_PORT}" >> ./gradle.properties \
    ; fi

RUN ./gradlew war

FROM tomcat:9.0-jre17

COPY --from=builder /home/otp/build/libs/otp.war /usr/local/tomcat/webapps/

EXPOSE 8080
