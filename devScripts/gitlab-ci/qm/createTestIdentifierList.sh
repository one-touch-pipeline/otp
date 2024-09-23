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

set -ev -o pipefail

TEST_IDENTIFIER_PATH_NAME="build/reports/test-identifier-list"
UNIT_TEST_PATH_NAME="${TEST_IDENTIFIER_PATH_NAME}/unit-test-identifier.txt"
INTEGRATION_TEST_PATH_NAME="${TEST_IDENTIFIER_PATH_NAME}/integration-test-identifier.txt"
WORKFLOW_TEST_PATH_NAME="${TEST_IDENTIFIER_PATH_NAME}/workflow-test-identifier.txt"
CYPRESS_TEST_PATH_NAME="${TEST_IDENTIFIER_PATH_NAME}/cypress-test-identifier.txt"

mkdir -p "${TEST_IDENTIFIER_PATH_NAME}"

find src/test/groovy/ -type f \
    \( -name "*Spec.groovy" -or -name "*Tests.groovy" -or -name "*Test.groovy" \) | \
    sort > "${UNIT_TEST_PATH_NAME}"

find src/integration-test/groovy/ -type f \
    \( -name "*Spec.groovy" -or -name "*Tests.groovy" -or -name "*Test.groovy" \) | \
    sort > "${INTEGRATION_TEST_PATH_NAME}"

find src/workflow-test/groovy/ -type f \
    \( -name "*Spec.groovy" -or -name "*Tests.groovy" \) -not -name "*Abstract*" | \
    sort > "${WORKFLOW_TEST_PATH_NAME}"

find cypress/e2e/ -type f -name "*.spec.js" | sort > "${CYPRESS_TEST_PATH_NAME}"

wc -l "${TEST_IDENTIFIER_PATH_NAME}"/*
