#!/bin/bash

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

red="\033[1;31m"
color_end="\033[0m"

# check coding style
# following checks are performed:
# keyword refers to one of the keywords: for, while, if, switch
# keyword(
# keyword  (
# keyword (foo){
# keyword (foo)  {
# {foo   except ${foo
keywords="for|while|if|switch|catch"
checks="((${keywords})(|\s\s+)\()|((${keywords})\s\(.*\)(|\s\s+)\{)|.*(?<!\$)\{[a-zA-Z0-9_\$]+"
if git diff --cached | sed -r '/^(-|\s|@@|\+\+\+|diff --git|index\s[a-f0-9]{7}\.\.[a-f0-9]{7}) */d' | grep -E -q "${checks}"; then
    echo -e "${red}Coding style violation${color_end}"
    git diff --cached | sed -r '/^(-|\s|@@|\+\+\+|diff --git|index\s[a-f0-9]{7}\.\.[a-f0-9]{7}) */d' | grep -E "${checks}"
    exit 1
fi
