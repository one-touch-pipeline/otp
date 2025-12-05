#!/bin/bash
#
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
#

# The script waits until the mocked cluster (image 'otp-mocked-cluster') serves everything the workflow tests need:
# - sshd: the image starts it only after the scripts in '/custom-cont-init.d' have prepared the test file system,
#   so an answering sshd also means the test data is unpacked
# - the mocked keycloak and WESkit servers: '50-start-services.sh' starts them in the background, therefore they
#   listen some time after sshd does
#
# The addresses are read from '~/.otp.properties', so the checked services are the ones OTP connects to during the
# tests. Without this check a service not being up yet ends in a failing test somewhere later, which is much harder
# to understand.

set -e -o pipefail

PROPERTIES=~/.otp.properties

# time to wait per service in seconds
TIMEOUT=60

# time between two attempts in seconds
INTERVAL=5

# reads a property from the otp properties, fails if it is missing or empty
readProperty() {
    local key="$1"
    local value

    value="$(sed -n -E "s/^[[:space:]]*${key//./\\.}[[:space:]]*=[[:space:]]*(.*)$/\1/p" "$PROPERTIES" | tail -n 1)"

    if [[ -z "$value" ]]
    then
        echo "the property '$key' is missing or empty in $PROPERTIES" >&2
        exit 1
    fi

    echo "$value"
}

# calls the given command until it succeeds, fails after $TIMEOUT
waitFor() {
    local name="$1"
    shift

    local end=$((SECONDS + TIMEOUT))

    until "$@" > /dev/null 2>&1
    do
        if (( SECONDS >= end ))
        then
            echo "$name is not available after ${TIMEOUT}s" >&2
            exit 1
        fi
        echo "waiting for $name"
        sleep "$INTERVAL"
    done

    echo "$name is available"
}

# ssh-keyscan exits with 0 and writes nothing if the server does not answer, therefore its output is checked
sshServerAvailable() {
    ssh-keyscan -p "$sshPort" "$sshHost" 2> /dev/null | grep -q "ssh-"
}

sshHost="$(readProperty 'otp.ssh.host')"
sshPort="$(readProperty 'otp.ssh.port')"
wesUrl="$(readProperty 'otp.wes.url')"
tokenUri="$(readProperty 'otp.wes.auth.tokenUri')"

waitFor "the ssh server on ${sshHost}:${sshPort}" sshServerAvailable

# the token uri is used unchanged by OTP, therefore it is also requested unchanged here.
# the mocked keycloak server answers only on POST, all other methods end in 404
waitFor "the mocked keycloak server on ${tokenUri}" curl --silent --fail --request POST "$tokenUri"

waitFor "the mocked WESkit server on ${wesUrl}" curl --silent --fail "${wesUrl%/}/service-info"
