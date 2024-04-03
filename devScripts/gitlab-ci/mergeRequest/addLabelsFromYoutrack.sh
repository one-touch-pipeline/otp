#!/bin/bash

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

# This script based on output of initMergeRequest.sh, which is used also in other scripts and should therefore executed outside of this script

set -e

if [ -v MERGE_REQUEST_EXIST ]
then
    echo "variable 'MERGE_REQUEST_EXIST' is not defined, please run initMergeRequest.sh"
    exit 1
fi

if [ "$MERGE_REQUEST_EXIST" == "true" ]
then
    exit 0
fi

echo "merge request"
MR_ID=`jq -e '.[0].iid' responseCheck.json`
echo "Merge request id: ${MR_ID}"

echo "get labels from youtrack"
source `dirname $0`/extractLabelsFromYoutrack.sh

if [[ "$LABEL_VALUES" == "" ]]
then
  echo "no labels to add"
  exit 0
fi

set +o pipefail
echo "add some labels from youtrack"
echo "$LABEL_VALUES" | \
  sed -e "s/,/\n/g" | \
  grep -e "sprint goal" -e "last sprint" -e "CRITICAL" -e "refactoring day" -e "Gamma" -e "otp issue meeting" -e "Guide dependency" | \
  sed -e "s/CRITICAL/priority::critical/g" \
      -e "s/Gamma/waiting for gamma/g" \
      -e "s/otp issue meeting/waiting for otp issue meeting/g" \
      -e "s/Guide dependency/coordinate with guide/g" | \
while read LABEL
do
  echo "add $LABEL"
  curl -X PUT --header "PRIVATE-TOKEN: $PROJECT_TOKEN" \
    --data-urlencode "add_labels=$LABEL" \
    "$PROJECT_URL/merge_requests/$MR_ID" > response.json
  echo "Response of add label '$LABEL' to MR"
  jq -C -e '.' response.json
done
