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
URL="https://one-touch-pipeline.myjetbrains.com/youtrack/api/issues/"
QUERY="?fields=id,resolved,summary,parent(issues(summary)),customFields(name,value(name))"

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
grep -v otp log-all.csv | sed -e "s/^/;/" > log-other.csv

set -e -o pipefail

#create unique otp-number list
sed -e 's/;.*//' log-otp.csv | sort -u  > otp-numbers.csv

#fetch data from youtrack
ISSUE=`cat otp-numbers.csv | tr '\n' ' '`
for OTP in $ISSUE
do
   JSON=`curl -X GET "$URL$OTP$QUERY"`
   EPIC=`echo $JSON | jq -Mc '.parent.issues[0].summary' | sed -e 's/\"//g' -e 's/null//'`
   STORY_POINTS=`echo $JSON | jq -MC '.customFields | map(select(.name == "Story points").value)[0]' | sed -e 's/null//'`
   LABEL_LAST_SPRINT=`echo $JSON | jq -Mc '.customFields | map(select(.name == "Label").value[]) | map(select(.name =="last sprint").name)[0]' | sed -e 's/\"//g' -e 's/null//'`
   LABEL_ADDED=`echo $JSON | jq -Mc '.customFields | map(select(.name == "Label").value[]) | map(select(.name =="reingezogen").name)[0]' | sed -e 's/\"//g' -e 's/null//'`
   SPRINTS=`echo $JSON | jq -Mc '.customFields | map(select(.name == "Sprints").value[].name) ' | sed -e 's/\"//g' -e 's/null//'`
   echo "$OTP;$EPIC;$STORY_POINTS;$LABEL_LAST_SPRINT;$LABEL_ADDED;$SPRINTS"
done > yt.csv

#combine tables
join -t ';' -1 1 -2 1 log-otp.csv yt.csv | sort -f -T ';' -k 4 -k 1 > otp-combined.csv

echo "OTP;TYPE;SUMMARY;EPIC;STORY_POINTS;LABEL_LAST_SPRINT;LABEL_REINGEZOGEN;SPRINTS" > header.tsv

cat header.tsv otp-combined.csv log-other.csv > final.tsv

cat final.tsv
