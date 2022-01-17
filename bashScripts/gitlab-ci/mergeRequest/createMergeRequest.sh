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

source `dirname $0`/initMergeRequest.sh

if [ "$NO_MERGE_REQUEST_EXIST" == "false" ]
then
  exit 0
fi

echo "check last commit message"
curl --header "PRIVATE-TOKEN: $PROJECT_TOKEN" "$PROJECT_URL/repository/commits/$CI_COMMIT_SHA" >  responseCommit.json
jq -C '.' responseCommit.json

DROP_CREATION="$(jq '.message | contains("[CreateNoRequest]")' responseCommit.json)"

if [ "$DROP_CREATION" == "true" ]
then
  echo 'no merge request created, since commit contains "[CreateNoRequest]"'
  exit 0
fi

echo "get labels from youtrack"
source `dirname $0`/extractLabelsFromYoutrack.sh

echo "create merge request"
curl -X POST --header "PRIVATE-TOKEN: $PROJECT_TOKEN" \
  --data-urlencode "title=$CI_COMMIT_TITLE" \
  --data-urlencode "source_branch=$CI_COMMIT_BRANCH" \
  --data-urlencode "target_branch=master" \
  --data-urlencode "assignee_id=$GITLAB_USER_ID" \
  --data-urlencode "description=$CI_COMMIT_DESCRIPTION" \
  --data-urlencode "squash=true" \
  --data-urlencode "remove_source_branch=true" \
  --data-urlencode "labels=waiting for author" \
  "$PROJECT_URL/merge_requests" > responseLabels.json

jq -C -e '.' responseLabels.json

MR_ID=$(jq -e '.iid' responseLabels.json)
echo "$MR_ID"

if [[ "$LABEL_VALUES" == "" ]]
then
  echo "no labels to add"
  exit 0
fi

echo "add some labels from youtrack"
echo "$LABEL_VALUES" | \
  sed -e "s/,/\n/g" | \
  grep -e "sprint goal" -e "last sprint" -e "CRITICAL" -e "refactoring day" | \
  sed -e "s/CRITICAL/priority::critical/g" | \
while read LABEL
do
  echo "add $LABEL"
  curl -X PUT --header "PRIVATE-TOKEN: $PROJECT_TOKEN" \
    --data-urlencode "add_labels=$LABEL" \
    "$PROJECT_URL/merge_requests/$MR_ID" > response.json
  jq -C -e '.' response.json
done
