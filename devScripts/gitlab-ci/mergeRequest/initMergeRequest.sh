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

set -e -o pipefail

if [[ ! -v PROJECT_TOKEN ]]
then
    echo "The variable PROJECT_TOKEN is not defined"
    exit 1
fi

if [[ ! -v PROJECT_URL ]]
then
    echo "The variable PROJECT_URL is not defined"
    exit 1
fi

set -vx

echo "search for existing merge request"
curl --header "PRIVATE-TOKEN: $PROJECT_TOKEN" "$PROJECT_URL/merge_requests?state=all&source_branch=$CI_COMMIT_BRANCH" >  responseCheck.json
jq -C '.' responseCheck.json

#does already a merge request exist?
NO_MERGE_REQUEST_EXIST="$(jq '. | length==0' responseCheck.json)"

echo "Are there no merge request found for this branch: $NO_MERGE_REQUEST_EXIST"
