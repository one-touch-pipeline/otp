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

if [ "$MERGE_REQUEST_EXIST" == "false" ]
then
    exit 0
fi

echo "merge request"
MR_ID=`jq -e '.[0].iid' responseCheck.json`
echo "Merge request id: ${MR_ID}"

echo "check merge request approvals"
curl --header "PRIVATE-TOKEN: $PROJECT_TOKEN" \
  "$PROJECT_URL/merge_requests/$MR_ID/approval_state" > responseApproval.json
jq -C -e '.' responseApproval.json

ALREADY_APPROVED=`cat responseApproval.json | jq -Mc '.rules | map(.approved_by) | map(select(.| length > 0)) | length > 0'`
echo $ALREADY_APPROVED

if [ "$ALREADY_APPROVED" == "true" ]
then
    LABEL="waiting for reviewer 2"
else
    LABEL="waiting for reviewer"
fi
echo $LABEL

curl -X PUT --header "PRIVATE-TOKEN: $PROJECT_TOKEN" \
  --data-urlencode "add_labels=$LABEL" \
  --data-urlencode "remove_labels=waiting for author" \
  "$PROJECT_URL/merge_requests/$MR_ID" > responseUpdate.json

echo "Response of add label '$LABEL' to and remove label 'waiting for author' from MR"
jq -C -e '.' responseUpdate.json
