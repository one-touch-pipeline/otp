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

# Script to run CodeNarc on all Groovy files changed in this branch
# Compares against the merge-base with origin/master
# Use this as a pre-commit hook to check your changes before committing

set -e

# Get list of changed files in this branch (modified, added, or renamed)
CHANGED_FILES=$(git diff --name-only "$(git merge-base HEAD origin/master)" --diff-filter=ACMR | grep '\.groovy$' | grep -v "^scripts/" | grep -v "^migrations/" || true)

# Check if there are any changed Groovy files
if [ -z "$CHANGED_FILES" ]; then
    echo "No Groovy files have been changed on this branch."
    exit 0
fi

# Count the files
FILE_COUNT=$(echo "$CHANGED_FILES" | wc -l)
echo "Found $FILE_COUNT changed Groovy file(s):"
echo "$CHANGED_FILES"
echo ""

# Convert newline-separated list to comma-separated list
FILES_CSV=$(echo "$CHANGED_FILES" | tr '\n' ',' | sed 's/,$//')

echo "Running CodeNarc on changed files..."
echo ""

# Run the gradle task with the files
./gradlew codenarcFiles -Pfiles="$FILES_CSV"

echo ""
echo "CodeNarc analysis complete!"
