#!/bin/bash
#
# Copyright 2011-2020 The OTP authors
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


# This script shows all new Codenarc violations introduced in your branch, if the number of violations exceeds the threshold.

if [ -n "$(git status --porcelain=v1 2>/dev/null)" ]; then
  echo "Uncommitted changed, exiting. Please commit your changes before running this script."
  exit 1
fi

gradle codenarcAll >/dev/null 2>&1

if [ $? -ne 0 ]; then
  mv build/reports/codenarc/all.txt build/reports/codenarc/all-new.txt
  # switch to the latest commit in origin/master that your branch is based on
  git checkout `git merge-base HEAD origin/master` >/dev/null 2>&1
  gradle codenarcAll >/dev/null 2>&1
  echo
  echo "New codenarc violations:"
  groovy scripts/codenarc-diff.groovy build/reports/codenarc/all.txt build/reports/codenarc/all-new.txt
  echo
  # switch back to your branch
  git checkout - >/dev/null 2>&1
else
  echo "No new codenarc violations."
fi
