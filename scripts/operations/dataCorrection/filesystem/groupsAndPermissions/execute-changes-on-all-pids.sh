#!/bin/bash

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

source ~/.config/otp/group-and-permission-cleanup/env.config
assertEnvSetup

if [ -z "$1" -o -z "$2" -o -z "$3" ];
then
    echo "not all parameters given, exiting"
    exit 1
fi

MODE="$1"
PID_FILE="$2"
SEARCHED_GROUP="$3"

SEPARATOR=","
TIMESTAMP="$(date '+%Y-%m-%d-%H-%M-%S')"
LOG_FILE_BASE="${LOG_DIR}/${TIMESTAMP}"

# grep to ignore comments and empty lines
grep -vE "(^#)|(^\s*$)" "$PID_FILE" | while IFS="$SEPARATOR" read -r pid seqTypeRaw group dir
do
    seqType="$(echo "$seqTypeRaw" | tr " " "_")"

    echo "Starting job for pid '${pid}' '${seqType}' (dir='${dir})'):"

    bash ./correct-unix-group-and-permission-on-pid-directory.sh "$MODE" "${PROJECT_ROOT}/${dir}/" "$SEARCHED_GROUP" "$group" > "${LOG_FILE_BASE}-${pid}-${seqType}-stdout" 2> "${LOG_FILE_BASE}-${pid}-${seqType}-stderr"
done
