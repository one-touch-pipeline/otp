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

git remote set-url origin "https://merge-request:${PROJECT_TOKEN}@${CI_SERVER_HOST}/${CI_PROJECT_PATH}.git"

ALL_BRANCHES=$(git for-each-ref 'refs/remotes/origin' --sort='-committerdate' --format='%(refname)|%(committerdate:short)')

CURRENT_SECONDS=$(date +%s)
THREE_MONTH_SECONDS=$((60 * 60 * 24 * 30 * 3))
CUTOFF_SECONDS=$(($CURRENT_SECONDS - $THREE_MONTH_SECONDS))

echo "Delete all branches without commits before $(date -d @$CUTOFF_SECONDS +"%d-%m-%Y") and without existing merge request"


while IFS='|' read -r BRANCH_NAME BRANCH_DATE; do
  export BRANCH_TO_CHECK=${BRANCH_NAME#refs/remotes/origin/}
  DATE_SECONDS=$(date -d ${BRANCH_DATE} +%s)

  if [ $CUTOFF_SECONDS -ge $DATE_SECONDS ]; then
    export MERGE_REQUEST_STATE=opened
    source "$(dirname $0)/mergeRequest/initMergeRequest.sh"

    if [ "$MERGE_REQUEST_EXIST" == "false" ]; then
      echo "Delete branch $BRANCH_TO_CHECK"
      git push origin --delete $BRANCH_TO_CHECK
    fi
  fi
done <<< "$ALL_BRANCHES"
