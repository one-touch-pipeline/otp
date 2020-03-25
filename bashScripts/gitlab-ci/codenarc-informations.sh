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

REPORT="build/reports/codenarc/all.txt"
METRICS="metrics.txt"

if [ ! -f "$REPORT" ]
then
    echo ''
    echo "file build/reports/codenarc/all.txt does not exist"
    echo ''
    exit -1
fi

LEVEL="1 2 3"

# Keep all codenarc metrics sorted together in the metrics report.
PREFIX="CodeNarc_"

echo ''
echo "Defined thresholds:"
grep -e "maxPriority.Violations" build.gradle | sed -re "s/^ *maxPriority([1-3])Violations = ([0-9]+)/\tMax. priority \1 violations: \2/" | tee \
            >( sed -re "s/\tMax. priority ([1-3]) violations: /${PREFIX}P\1_MaxViolations /" >> "$METRICS" )

echo ''
echo "Found violations:"
grep --max-count=1 -e "^Summary: TotalFiles=" "$REPORT" | sed -re "s/ P([1-3]+)=([0-9]+)/\n\tFound priority \1 violations: \2/g" |  grep -v Summary | tee \
            >( sed -re "s/\tFound priority ([1-3]) violations: /${PREFIX}P\1_FoundViolations /" >> "$METRICS" )

echo ''
echo "Count of rules with violations:"
for p in $LEVEL; do
    grep -oEe "Violation: Rule=[^ ]+ P=$p " "$REPORT" | sort -u | wc -l | tee \
            >( sed -e "s/^/${PREFIX}P${p}_DistinctRulesViolated /" >> "$METRICS" ) \
            |  sed -e "s/^/\tDistinct P$p rules violated: /"
done

for p in $LEVEL; do
    echo ''
    echo "Rules of priority $p with violations count:"
    grep -oEe "Violation: Rule=[^ ]+ P=$p " "$REPORT" | sed -e "s/Violation: Rule=//" -e "s/ P=[1-3]//" | sort | uniq -c | sort -k1,1hr | \
            tee >( sed -re "s/ +([0-9]+) ([a-zA-Z]+)/${PREFIX}P${p}_Violations_\2 \1/" >> "$METRICS" )
done

echo ''
echo "Show level 1 violations"
cat "$REPORT"  | grep -e "^File: " -e " P=1 " | grep -B 1 "Violation:" | sed -e "s/--//"

echo ''
