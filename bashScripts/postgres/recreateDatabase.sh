#!/bin/bash

# Copyright 2011-2019 The OTP authors
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


#######################################################################
# Script to create a local development database in a docker container
#
# by default, loads the most recent database dump in $LATEST_DUMP_LOCATION into the local otp database.
# to load other dumps, specify a search pattern in $USE_DUMP, like so:
#    USE_DUMP=<SEARCH> ./bashScripts/postgres/recreateDatabase.sh
# <SEARCH> can be any substring of a dump-identifier, and is glob-expanded on both ends to find a full dump string.
# when multiple dumps match the search pattern, the youngest match is taken.
# recommended search strings are "YYYY-MM-DD", "allData_YYYY-MM-DD" or "YYYY-MM-DD_hh-mm-ss" (or any part/combination thereof)
#
# examples:
#   USE_DUMP=2019-02-12          ./bashScripts/postgres/recreateDatabase.sh
#   USE_DUMP=allData_2019-02-12  ./bashScripts/postgres/recreateDatabase.sh
#   USE_DUMP=2019-02-12_09-52-22 ./bashScripts/postgres/recreateDatabase.sh

set -euo pipefail

trap 'error_handler' SIGINT SIGTERM SIGQUIT EXIT

error_handler() {
    [ $? -eq 0 ] && exit
    echo "Aborting..."
    stop_and_destroy_container
    exit
}

docker_compose() {
    # Suppress output to mimic the output of the other script
    docker-compose -f docker/otp/docker-compose.yml $* >/dev/null 2>&1
}

rebuild_image() {
    docker_compose build --pull
}

start_container() {
    docker_compose up -d
}

stop_and_destroy_container() {
    docker_compose stop
    docker_compose rm -f -v
    echo "Database deleted"
}

stop_and_destroy_container
rebuild_image
start_container
echo "Database created"

if [[ ! -v LATEST_DUMP_LOCATION ]] || [[ ! ${LATEST_DUMP_LOCATION} ]]; then
    echo "Dump location not set, leaving database empty."
    exit
fi

if [[ ! -d ${LATEST_DUMP_LOCATION} ]] || [[ ! -r ${LATEST_DUMP_LOCATION} ]]; then
    echo "Dump location not found or not accessible, leaving database empty."
    exit
fi

# Wait a few seconds for the server to come up. Just in case.
sleep 10

# see if USE_DUMP override is present and not-empty.
if [[ ${USE_DUMP:+foo} ]]; then
    echo "looking for dump matching '${USE_DUMP}' .."
    DUMP_TO_LOAD=`ls -dt "${LATEST_DUMP_LOCATION}/"*"${USE_DUMP}"*.dump | head -n1`
    # aborts here (thanks to set+e) if `ls` doesn't find a matching expansion: "ls: cannot access /ibios/dmdc/otp/postgres/dumps/production/*XXXXXX*.dump: No such file or directory"

    echo " .. found '${DUMP_TO_LOAD}'"
else
    DUMP_TO_LOAD=`ls -dt "${LATEST_DUMP_LOCATION}"/*.dump | head -n1`
    echo "Using default latest dump: $DUMP_TO_LOAD"
fi
du -hs ${DUMP_TO_LOAD}

# Work around pg_restore failing due to an option set automatically by Postgres clients >= 9.3
echo "Loading dump..."
time pg_restore --username=postgres --host=localhost --dbname=otp --jobs=4 ${DUMP_TO_LOAD} || true
echo "Dump loaded"

PSQL="psql --username=otp --host=localhost --dbname=otp"
echo "Disabling workflows plans"
${PSQL} --command "update job_execution_plan set enabled='f' where enabled='t';"
echo "Marking all not finished workflows as finished"
${PSQL} --command "UPDATE public.process SET finished='true' WHERE finished='false';"

echo ""
