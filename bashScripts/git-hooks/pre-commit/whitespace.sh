#!/bin/sh

red="\033[1;31m"
color_end="\033[0m"
echo ws
# Check unwanted trailing whitespace or space/tab indents;

if [[ `git diff --cached --check` ]]; then
    echo -e ${red}Commit failed${color_end}
    git diff --cached --check
   exit 1
fi
