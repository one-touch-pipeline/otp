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


if [ ! -f build/reports/codenarc/all.txt ]
then
    echo ''
    echo "file build/reports/codenarc/all.txt does not exist"
    echo ''
    exit -1
fi

LEVEL="1 2 3"

echo ''
echo "Defined thresholds:"
cat  build.gradle  | grep -e "maxPriority.Violations" | sed -e "s/ *maxPriority/\tmax priority /g" -e "s/Violations = / violations: /g"

echo ''
echo "Found violations:"
head build/reports/codenarc/all.txt | grep -e "^Summary: TotalFiles=" | sed -e "s/ P/\n\tfound priority /g" -e "s/=/ violations: /g" |  grep -v Summary

echo ''
echo "Count of rules with violations:"
for p in $LEVEL
do
    cat build/reports/codenarc/all.txt | grep Violation | grep -v Summary | grep " P=$p " | awk '{print $2}' | sort -u | wc -l | sed -e "s/^/\trule count of priority $p violations: /"
done

for p in $LEVEL
do
    echo ''
    echo "Rules of priority $p with violations count:"
    cat build/reports/codenarc/all.txt | grep Violation | grep -v Summary | grep " P=$p " | awk '{print $2}' | sed -e "s/Rule=//" -e "s/P=//g" | sort | uniq -c | sort -k1,1hr
done

echo ''
echo "Show level 1 violations"
cat build/reports/codenarc/all.txt  | grep -e "^File: " -e " P=1 " | grep -B 1 "Violation:" | sed -e "s/--//"

echo ''
