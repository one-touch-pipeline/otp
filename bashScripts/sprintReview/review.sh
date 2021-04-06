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

set -ev

#constants
BASE_URL="https://one-touch-pipeline.myjetbrains.com/youtrack/api"
ISSUE_FIELDS="id,idReadable,resolved,summary,parent(issues(summary)),customFields(name,value(name))"

ISSUE_URL="${BASE_URL}/issues/"
ISSUE_SELECTION="?fields=$ISSUE_FIELDS"

SPRINT_URL="${BASE_URL}/agiles/106-4"
BOARD_SELECTION="?fields=id,name,currentSprint(name,issues($ISSUE_FIELDS))"

#work directory
mkdir -p review
cd review

#last review tag
set +e
LATEST_REVIEW_TAG=$(git describe --abbrev=0 --tags --match "sprint_review_*" HEAD^)
set -e
if [[ -z "${LATEST_REVIEW_TAG}" ]]
then
  git fetch --unshallow --tags
  LATEST_REVIEW_TAG=$(git describe --abbrev=0 --tags --match "sprint_review_*" HEAD^)
fi
echo LATEST_REVIEW_TAG

#get git history
git log $LATEST_REVIEW_TAG..HEAD --pretty=format:'%s' | sed -e 's/: /;/g' > log-all.csv

set +e

#find issues with otp number
grep otp log-all.csv | awk -F ';' '{print $2";"$1";"$3}' | sort > log-otp.csv

#find issues without otp number
grep -v otp log-all.csv | sed -e "s/^/;/" | sort > log-other.csv

set -e -o pipefail

#create unique otp-number list
sed -e 's/;.*//' log-otp.csv | sort -u  > otp-numbers.csv
COMMIT_OTP_NUMBERS=`cat otp-numbers.csv | tr '\n' ' '`

#fetch board
AGILE_BOARD=`curl -X GET "${SPRINT_URL}${BOARD_SELECTION}"`
export SPRINT_NAME=`echo $AGILE_BOARD | jq -Mc '.currentSprint.name' | sed -e 's/"//g' `
ISSUES_BOARD=`echo $AGILE_BOARD | jq -Mc '.currentSprint.issues'`
FINISHED_ISSUES_BOARD=`echo $ISSUES_BOARD | jq -Mc 'map(select(.customFields[] | select(.name=="State" and .value.name=="Done")))'`
SPRINT_OTP_NUMBERS=`echo $ISSUES_BOARD | jq -Mc 'map(.idReadable) | .[]' | sed -e 's/"//g' | tr '\n' ' '` ;
FINISHED_OTP_NUMBERS=`echo $FINISHED_ISSUES_BOARD | jq -Mc 'map(.idReadable) | .[]' | sed -e 's/"//g' | tr '\n' ' '` ;


#collect all numbers
ALL_OTP_NUMBERS=`echo $FINISHED_OTP_NUMBERS $COMMIT_OTP_NUMBERS | tr ' ' '\n' | sort -u | tr '\n' ' '`
echo "ALL NUMBERS: '$ALL_OTP_NUMBERS'"

#create table
for OTP in $ALL_OTP_NUMBERS
do
    echo "" >&2
    echo "Check $OTP" >&2

    #export otp, so jq can access it. Combine it before in the string don't work
    export OTP

    #get data from board cache or from youtravk(if finished already in last sprint)
    if [[ "$SPRINT_OTP_NUMBERS" == *"$OTP"* ]]
    then
        JSON=`echo $ISSUES_BOARD | jq -Mc 'map(select(.idReadable ==$ENV.OTP))[0]'` ;
    else
        JSON=`curl -X GET "${ISSUE_URL}${OTP}${ISSUE_SELECTION}"`
    fi

    #extract information for table
    SUMMARY=`echo $JSON | jq -Mc '.summary' | sed -e 's/\"//g' -e 's/null//'`
    EPIC=`echo $JSON | jq -Mc '.parent.issues[0].summary' | sed -e 's/\"//g' -e 's/null//'`
    STORY_POINTS=`echo $JSON | jq -Mc '.customFields | map(select(.name == "Story points").value)[0]' | sed -e 's/null//'`

    SPRINTS=`echo $JSON | jq -Mc '.customFields | map(select(.name == "Sprints").value[].name) '`
    SPRINT_NAMES=`echo $SPRINTS | sed -e 's/\"//g' -e 's/null//'`
    IS_LAST_SPRINT=`echo $SPRINTS | jq -Mc 'if map(select(. == $ENV.SPRINT_NAME)) == [] then "LAST" else "CURRENT" end'`
    IS_FINISHED=`echo $JSON | jq -Mc '.customFields | map(select(.name=="State").value.name)[0] | if . == "Done" then "Done" else "Working" end' | sed -e 's/\"//g'`

    LABEL_LAST_SPRINT=`echo $JSON | jq -Mc '.customFields | map(select(.name == "Label").value[]) | map(select(.name =="last sprint").name)[0]' | sed -e 's/\"//g' -e 's/null//'`
    LABEL_ADDED=`echo $JSON | jq -Mc '.customFields | map(select(.name == "Label").value[]) | map(select(.name =="reingezogen").name)[0]' | sed -e 's/\"//g' -e 's/null//'`

    #create table row
    echo "$OTP; ; ;$EPIC;$SUMMARY;$IS_FINISHED;$IS_LAST_SPRINT;$STORY_POINTS;$SPRINT_NAMES;$LABEL_LAST_SPRINT;$LABEL_ADDED"
done > yt.csv

#combine tables
join -t ';' -1 1 -2 1 log-otp.csv yt.csv > otp-joined.csv

#create file with issues having no commit
grep -v -F -f otp-numbers.csv  yt.csv | sed -e "s/;/; ; ;/" > otp-no-commit.csv

#sort joined and no commit together
sort -f -t ';' -k 6 -k 1.5n otp-joined.csv otp-no-commit.csv > otp-combined.csv

#header
echo "OTP;TYPE;COMMIT;GUI;THEME;EPIC;SUMMARY;IS_FINISHED;IS_LAST_SPRINT;STORY_POINTS;ALL_SPRINTS;LABEL_LAST_SPRINT;LABEL_REINGEZOGEN" > header.csv

#create final table
cat header.csv otp-combined.csv log-other.csv > final.csv

echo "-------------------------------"
cat final.csv
