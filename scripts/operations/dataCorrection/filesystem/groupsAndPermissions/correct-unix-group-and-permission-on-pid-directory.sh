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

set -v

if [ -z "$1" -o -z "$2" -o -z "$3" -o -z "$4" ];
then
    echo "not all parameters given, exiting"
    exit 1
fi

MODE="$1"
PID_DIRECTORY="$2"
UNIX_GROUP_IS="$3"
UNIX_GROUP_SHOULD="$4"


CMD_CHGRP="chgrp -vh "$UNIX_GROUP_SHOULD" {}"
CMD_CHMOD_2750="chmod -v 2750 {}"
CMD_CHMOD_400="chmod -v 400 {}"
CMD_CHMOD_440="chmod -v 440 {}"
CMD_CHMOD_444="chmod -v 444 {}"
CMD_CHMOD_640="chmod -v 640 {}"

interceptor=""

case "$MODE" in
    "list")
        interceptor="echo"
        ;;
    "clean")
        interceptor=""
        ;;
    *)
        echo "unknown mode ${MODE}, use 'list' or 'clean'"
        exit 2
        ;;
esac


if [ -d "$PID_DIRECTORY" ]
then
    find "$PID_DIRECTORY" \
        \( -group "$UNIX_GROUP_IS"                                        -exec $interceptor $CMD_CHGRP \; \) , \
        \( -type d -not -perm 2750                                        -exec $interceptor $CMD_CHMOD_2750 \; \) , \
        \( -type f -not -perm 440 \
                   -not -name "*.bam" \
                   -not -name "*.bai" \
                   -not -name ".roddyExecCache.txt" \
                   -not -name "zippedAnalysesMD5.txt" \
                   -not -name "*_mapping.tsv"                              -exec $interceptor $CMD_CHMOD_440 \; \) , \
        \( -type f -not -perm 444 \( -name "*.bam" -or -name "*.bai" \)    -exec $interceptor $CMD_CHMOD_444 \; \) , \
        \( -type f -not -perm 640 -name "*_mapping.tsv" -path "*/0_all/*"  -exec $interceptor $CMD_CHMOD_640 \; \);
else
    echo "# pid directory not found: $PID_DIRECTORY"
fi
