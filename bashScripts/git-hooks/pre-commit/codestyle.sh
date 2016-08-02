#!/bin/sh

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
checks="((${keywords})(|\s\s+)\()|((${keywords})\s\(.*\)(|\s\s+)\{)|.*(?<!\$)\{[a-Z0-9_\$]+"
if [[ `git diff --cached | sed -r '/^(-|\s|@@|\+\+\+|diff --git|index\s[a-f0-9]{7}\.\.[a-f0-9]{7}) */d' | egrep ${checks}` ]]; then
    echo -e ${red}Coding style violation${color_end}
    git diff --cached | sed -r '/^(-|\s|@@|\+\+\+|diff --git|index\s[a-f0-9]{7}\.\.[a-f0-9]{7}) */d' | egrep ${checks}
    exit 1
fi
