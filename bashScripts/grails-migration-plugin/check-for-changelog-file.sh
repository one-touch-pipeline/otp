#!/bin/bash

set -e

year=`date +'%Y'`

if [ -f  migrations/changelogs/${year}/testing.groovy ]; then
    cat migrations/changelogs/${year}/testing.groovy
    exit 1
fi
