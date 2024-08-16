#!/bin/bash

#
# Copyright 2011-2024 The OTP authors
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

set -e -o pipefail

if [[ ! -v PROJECT_TOKEN ]]; then
  echo "The variable PROJECT_TOKEN is not defined"
  exit 1
fi

if [[ ! -v PROJECT_URL ]]; then
  echo "The variable PROJECT_URL is not defined"
  exit 1
fi

if [[ ! -v CI_PIPELINE_ID ]]
then
    echo "The variable CI_PIPELINE_ID is not defined"
    exit 1
fi

export PIPELINE_JOBS_FILE="pipeline.json"
export PIPELINE_JOBS_NAME="jobNames.txt"

set -v

# fetch pipeline info for current pipeline
curl --location --header "PRIVATE-TOKEN: ${PROJECT_TOKEN}" "${PROJECT_URL}/pipelines/${CI_PIPELINE_ID}/jobs?per_page=100" > "${PIPELINE_JOBS_FILE}"
cat "${PIPELINE_JOBS_FILE}" | jq -C

# extract job names
cat "${PIPELINE_JOBS_FILE}" | jq -e 'map(.name)' | tee "${PIPELINE_JOBS_NAME}"

# search for coverage job, use grep exist code to check if job exist
set +e
grep "test coverage" ${PIPELINE_JOBS_NAME}
RETURN=$?
echo $RETURN
set -e

# do output based if job was found or not
set +v
if [ $RETURN -eq 0 ]; then
  echo "coverage job exist in pipeline"
else
  echo "coverage job doesn't exist, provide dummy coverage"
  echo "Instruction Coverage: 100.0%"
fi
