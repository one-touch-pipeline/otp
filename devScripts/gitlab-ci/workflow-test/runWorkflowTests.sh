#!/bin/bash
#
# Copyright 2011-2026 The OTP authors
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


# The script runs the workflow tests of the given group.
#
# The workflow tests are long running, therefore they are split into groups, which the gitlab job 'workflow tests'
# runs in parallel, one parallel job per group, see '.gitlab-ci-core.yml'.

set -e -o pipefail

group="${1:-}"

case "$group" in
    IMPORT)
        tests=(
            'de.dkfz.tbi.otp.workflowTest.dataInstallation.*'
            'de.dkfz.tbi.otp.workflowTest.bamImport.*'
        )
        ;;
    FASTQC)
        tests=( 'de.dkfz.tbi.otp.workflowTest.fastqc.*' )
        ;;
    ALIGNMENT)
        tests=( 'de.dkfz.tbi.otp.workflowTest.alignment.roddy.*' )
        ;;
    SNV)
        tests=( 'de.dkfz.tbi.otp.workflowTest.analysis.roddy.snvcalling.*' )
        ;;
    INDEL)
        tests=( 'de.dkfz.tbi.otp.workflowTest.analysis.roddy.indelcalling.*' )
        ;;
    SOPHIA)
        tests=( 'de.dkfz.tbi.otp.workflowTest.analysis.roddy.sophia.*' )
        ;;
    ACESEQ)
        tests=( 'de.dkfz.tbi.otp.workflowTest.analysis.roddy.aceseq.*' )
        ;;
    *)
        echo "unknown workflow test group '${group}', see the matrix of the job 'workflow tests' in '.gitlab-ci-core.yml'" >&2
        exit 1
        ;;
esac

# the patterns are quoted, so the shell does not expand them
arguments=()
for test in "${tests[@]}"
do
    arguments+=( '--tests' "$test" )
done

echo "run the workflow tests of the group ${group}: ${tests[*]}"

./gradlew --build-cache workflowTest "${arguments[@]}"
