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

# The script copies the logs of the mocked servers, which run in the mocked cluster (image 'otp-mocked-cluster'),
# into the artifact directory, so they can be analysed after the job has finished.
#
# The script is used in the after script and therefore never fails the job: if the logs cannot be fetched, it only
# writes a warning, since a failing log collection should not hide the result of the tests.

set -e -o pipefail

PROPERTIES=~/.otp.properties

# the value of '$LOGS' of the image 'otp-mocked-cluster', see 'docker/mocked-workflow-test/Dockerfile'.
# it is hardcoded, since sshd does not pass the environment of the container to the ssh sessions
REMOTE_LOGS=/workflows/logs

# the artifacts of the job must be inside the project directory, therefore the logs cannot be put in the home directory
TARGET="${CI_PROJECT_DIR:-.}/mocked-servers"

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

sshHost="$(readProperty 'otp.ssh.host')"
sshPort="$(readProperty 'otp.ssh.port')"
sshUser="$(readProperty 'otp.ssh.user')"

# the tests connect with a password and 'sshpass' is not installed, therefore the password is given to ssh via an
# askpass helper. 'SSH_ASKPASS_REQUIRE=force' makes ssh use it also without a terminal.
# the password itself is passed in the environment, so it is not written to disk
export MOCKED_CLUSTER_PASSWORD
MOCKED_CLUSTER_PASSWORD="$(readProperty 'otp.ssh.password')"

askpass="$(mktemp)"
trap 'rm -f "$askpass"' EXIT
printf '#!/bin/bash\n\nprintf "%%s\\n" "$MOCKED_CLUSTER_PASSWORD"\n' > "$askpass"
chmod 700 "$askpass"

export SSH_ASKPASS="$askpass"
export SSH_ASKPASS_REQUIRE=force
export DISPLAY=none

mkdir -p "$TARGET"

# the remote directory itself is copied, therefore the copy is named like it
if scp -r -P "$sshPort" "${sshUser}@${sshHost}:${REMOTE_LOGS}" "$TARGET"
then
    echo "copied the logs of the mocked servers to ${TARGET}/$(basename "$REMOTE_LOGS")"
    ls -l "${TARGET}/$(basename "$REMOTE_LOGS")" || true
else
    echo "WARNING: could not copy ${sshUser}@${sshHost}:${REMOTE_LOGS} to $TARGET" >&2
fi
