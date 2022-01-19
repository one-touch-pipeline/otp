#!/bin/bash
# Copyright 2011-2021 The OTP authors
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

set -ev -o pipefail

#constants
BASE_URL="https://one-touch-pipeline.myjetbrains.com/youtrack/api"
ISSUE_FIELDS="id,idReadable,customFields(name,value(name))"

ISSUE_URL="${BASE_URL}/issues/"
ISSUE_SELECTION="?fields=$ISSUE_FIELDS"

SPRINT_URL="${BASE_URL}/agiles/106-4"
BOARD_SELECTION="?fields=id,name,currentSprint(name,issues($ISSUE_FIELDS))"

#fork commit of branch
BRANCH_START=`git merge-base HEAD origin/master`

#get first otp number of branch
set +o pipefail
OTP_NUMBER=`git log $BRANCH_START..HEAD --pretty=format:'%s' | \
  sed -e 's/: /;/g' -e '/^\(Revert\|Merge\)/d' | \
  grep -e "otp-" | \
  awk -F ';' '{print $2}' | \
  tail -n 1`
set -o pipefail

echo $OTP_NUMBER
if [[ -z "$OTP_NUMBER" ]]
then
    echo "No OTP issue available, skip label extraction"
else
  echo "Extract label(s) from youtrack for $OTP_NUMBER"
  JSON=`curl -X GET "${ISSUE_URL}${OTP_NUMBER}${ISSUE_SELECTION}"`
  echo $JSON | jq -C

  LABEL_VALUES=`echo $JSON | jq -Mc '.customFields | map(select(.name == "Label").value[].name )' | \
      sed -e 's/\"//g' -e 's/\[//'  -e 's/\]//'`
  echo "Extracted labels: $LABEL_VALUES"
fi
