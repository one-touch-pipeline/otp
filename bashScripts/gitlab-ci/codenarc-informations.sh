#!/bin/bash

echo "Defined thresholds:"
cat  grails-app/conf/BuildConfig.groovy  | grep -e "maxPriority.Violations" | sed -e "s/ *maxPriority/\tmax priority /g" -e "s/Violations = / violations: /g"

echo "Found violations:"
head target/CodeNarc-Report.txt | grep -e "^Summary: TotalFiles=" | sed -e "s/ P/\n\tfound priority /g" -e "s/=/ violations: /g" |  grep -v Summary

echo "Count of rules with violations:"
for p in 1 2 3 4
do
    cat target/CodeNarc-Report.txt | grep Violation | grep -v Summary | grep " P=$p " | awk '{print $2}' | sort -u | wc -l | sed -e "s/^/\trule count of priority $p violations: /"
done

echo "Rules with violations count (priority in brace):"
cat target/CodeNarc-Report.txt | grep Violation | grep -v Summary | awk '{print $2"("$3")"}' | sed -e "s/Rule=//" -e "s/P=//g" | sort | uniq -c | sort -k1,1hr
