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
# This Script can receive the variable BRANCH_TO_CHECK and MERGE_REQUEST_STATE, to check, whether a merge request in the given state exists for that branch.
# When no BRANCH_TO_CHECK is passed the $CI_COMMIT_BRANCH variable from gitlab will be used.
# When no MERGE_REQUEST_STATE is passed, it will check for any merge request.

if [[ ! -v PROJECT_TOKEN ]]; then
  echo "The variable PROJECT_TOKEN is not defined"
  exit 1
fi

if [[ ! -v PROJECT_URL ]]; then
  echo "The variable PROJECT_URL is not defined"
  exit 1
fi

if [[ ! -v BRANCH_TO_CHECK ]]; then
  BRANCH_TO_CHECK=$CI_COMMIT_BRANCH
fi

if [[ ! -v MERGE_REQUEST_STATE ]]; then
  MERGE_REQUEST_STATE=all
fi

echo "Search for an existing merge request for branch $BRANCH_TO_CHECK with state $MERGE_REQUEST_STATE"
curl --header "PRIVATE-TOKEN: $PROJECT_TOKEN" "$PROJECT_URL/merge_requests?state=${MERGE_REQUEST_STATE}&source_branch=$BRANCH_TO_CHECK" >responseCheck.json
jq -C '.' responseCheck.json

MERGE_REQUEST_EXIST=$(jq '. | length!=0' responseCheck.json)

if [ "$MERGE_REQUEST_EXIST" == "true" ]; then
  echo "A Merge request was found for the branch $BRANCH_TO_CHECK"
else
  echo "No Merge requests were found for the branch $BRANCH_TO_CHECK"
fi
