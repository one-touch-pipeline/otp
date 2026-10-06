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

# The script copies the following directories of the mocked cluster (image 'otp-mocked-cluster') into the artifact
# directory, so they can be analysed after the job has finished:
# - the logs of the mocked servers, always
# - the file system the workflow tests have created and the jobs of the mocked cluster, only if the job did not
#   succeed, since they are only needed to analyse failing tests
#
# If a directory cannot be fetched, the script writes a warning and continues with the next one, so as many directories
# as possible are collected. At the end it fails, if at least one directory could not be fetched.

set -e -o pipefail

PROPERTIES=~/.otp.properties

# the values of '$LOGS', '$WORKFLOWS/tests' and '$JOBS' of the image 'otp-mocked-cluster',
# see 'docker/mocked-workflow-test/Dockerfile'.
# they are hardcoded, since sshd does not pass the environment of the container to the ssh sessions
REMOTE_LOGS=/workflows/logs
REMOTE_TESTS=/workflows/tests
REMOTE_JOBS=/workflows/jobs

# the artifacts of the job must be inside the project directory, therefore the directories cannot be put in the home directory
TARGET_LOGS="${CI_PROJECT_DIR:-.}/mocked-servers"
TARGET_TESTS="${CI_PROJECT_DIR:-.}/mocked-tests"
TARGET_JOBS="${CI_PROJECT_DIR:-.}/mocked-jobs"

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

# set to 1 if at least one directory could not be copied
failed=0

# copies a directory of the mocked cluster into the given target directory.
# a failure is only recorded in 'failed', so the following directories are still copied
copyRemoteDirectory() {
    local remote="$1"
    local target="$2"
    local description="$3"

    if ! mkdir -p "$target"
    then
        echo "WARNING: could not create $target, therefore ${remote} is not copied" >&2
        failed=1
        return 0
    fi

    # the remote directory itself is copied, therefore the copy is named like it
    if scp -r -P "$sshPort" "${sshUser}@${sshHost}:${remote}" "$target"
    then
        echo "copied ${description} to ${target}/$(basename "$remote")"
        ls -l "${target}/$(basename "$remote")" || true
    else
        echo "WARNING: could not copy ${sshUser}@${sshHost}:${remote} to $target" >&2
        failed=1
    fi
}

copyRemoteDirectory "$REMOTE_LOGS" "$TARGET_LOGS" 'the logs of the mocked servers'

# 'CI_JOB_STATUS' is only defined in the after script of a gitlab job. If it is unset, the script is called manually
# and the directories are fetched, since that is done to analyse something.
if [[ "${CI_JOB_STATUS:-}" == "success" ]]
then
    echo "skipped ${REMOTE_TESTS} and ${REMOTE_JOBS}, they are only needed to analyse failing tests and the job succeeded"
else
    copyRemoteDirectory "$REMOTE_TESTS" "$TARGET_TESTS" 'the file system created by the workflow tests'
    copyRemoteDirectory "$REMOTE_JOBS" "$TARGET_JOBS" 'the jobs of the mocked cluster'
fi

if [[ "$failed" -ne 0 ]]
then
    echo "ERROR: at least one directory of the mocked cluster could not be copied, see the warnings above" >&2
    exit 1
fi
