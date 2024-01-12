#!/bin/bash

# Copyright 2011-2019 The OTP authors
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

set -e

source `dirname $0`/initMergeRequest.sh

if [ "$NO_MERGE_REQUEST_EXIST" == "true" ]
then
    echo "Exiting, because no merge request exists."
    exit 0
fi

echo "merge request"
MR_ID=`jq -e '.[0].iid' responseCheck.json`
echo "Merge request id: ${MR_ID}"

curl -X PUT --header "PRIVATE-TOKEN: $PROJECT_TOKEN" \
  --data-urlencode "add_labels=waiting for author" \
  "$PROJECT_URL/merge_requests/$MR_ID" > responseUpdate.json

echo "Response of add label 'waiting for author' to MR"
jq -C -e '.' responseUpdate.json
