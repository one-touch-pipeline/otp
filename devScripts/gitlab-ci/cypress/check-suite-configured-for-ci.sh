#!/bin/sh
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


# Check, that all suites of the cypress is also configured to run with the ci

set -ev -o pipefail

# go through all suites (dirs in e2e directors)
find cypress/e2e/ -mindepth 1 -maxdepth 1 -type d | sed -e 's#cypress/e2e/##g' | sort | while IFS= read -r i
do
  echo check for $i
  # check, that a cypress test is defined for that suite
  grep "\"$i\"" .gitlab-ci-core.yml || ( echo "suite '"$i"' not configured to run with ci" && exit 1 )
done

echo "All cypress suites are defined as gitlab ci job"
