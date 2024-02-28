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

# This script add the author as assignee, if no assignee is set yet.

set -e

if [ -v NO_MERGE_REQUEST_EXIST ]
then
    echo "variable 'NO_MERGE_REQUEST_EXIST' is not defined, please run initMergeRequest.sh"
    exit 1
fi

if [ "$NO_MERGE_REQUEST_EXIST" == "true" ]
then
    exit 0
fi

echo "merge request"
MR_ID=`jq -e '.[0].iid' responseCheck.json`
echo "Merge request id: ${MR_ID}"

ASSIGNEES=`jq -e '.[0].assignees' responseCheck.json`
echo "Merge request assignees: ${ASSIGNEES}"

HAS_ASSIGNEES=`echo $ASSIGNEES | jq -Mc '. | length > 0'`
echo $HAS_ASSIGNEES

if [ "$HAS_ASSIGNEES" == "true" ]
then
    exit
fi

AUTHOR=`jq -e '.[0].author' responseCheck.json`
echo "Merge request author: ${AUTHOR}"

AUTHOR_ID=`jq -e '.[0].author.id' responseCheck.json`
echo "Merge request author id: ${AUTHOR_ID}"

curl -X PUT --header "PRIVATE-TOKEN: $PROJECT_TOKEN" \
  --data-urlencode "assignee_ids=$AUTHOR_ID" \
  "$PROJECT_URL/merge_requests/$MR_ID" > responseUpdate.json

echo "Response of set assignees from MR"
jq -C -e '.' responseUpdate.json
