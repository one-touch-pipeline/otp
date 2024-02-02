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

#######################################################################
# Script to create a local development database
#
# by default, loads the most recent database dump in $LATEST_DUMP_LOCATION into the local otp database.
# to load other dumps, specify a search pattern in $USE_DUMP, like so:
#    USE_DUMP=<SEARCH> ./devScripts/postgres/recreateDatabase.sh
# <SEARCH> can be any substring of a dump-identifier, and is glob-expanded on both ends to find a full dump string.
# when multiple dumps match the search pattern, the youngest match is taken.
# recommended search strings are "YYYY-MM-DD", "allData_YYYY-MM-DD" or "YYYY-MM-DD_hh-mm-ss" (or any part/combination thereof)
#
# examples:
#   ./devScripts/postgres/recreateDatabase.sh                # default: loads most recent dump
#   ./devScripts/postgres/recreateDatabase.sh 2019-02-12
#   ./devScripts/postgres/recreateDatabase.sh allData_2019-02-12
#   ./devScripts/postgres/recreateDatabase.sh 2019-02-12_09-52-22

set -Eeux

if [[ ! -v LATEST_DUMP_LOCATION ]] || [[ ! ${LATEST_DUMP_LOCATION} ]]; then
    echo "Dump location not set, leaving database empty."
    exit
fi

if [[ ! -d ${LATEST_DUMP_LOCATION} ]] || [[ ! -r ${LATEST_DUMP_LOCATION} ]]; then
    echo "Dump location not found or not accessible, leaving database empty."
    exit
fi

if [[ ! -v PRODUCTION_POSTGRES_JOB_COUNT ]]; then
  PRODUCTION_POSTGRES_JOB_COUNT=4
fi

# Wait a few seconds for the server to come up. Just in case.
sleep 10

# get port from docker
PORT=$(docker inspect --format='{{range $p, $conf := .NetworkSettings.Ports}}{{(index $conf 0).HostPort}} {{end}}' database_postgres_1)

# see if an override parameter is present
if [ $# -gt 0 ]; then
    echo "looking for dump matching '$1' .."
    DUMP_TO_LOAD=$( ls -dt "${LATEST_DUMP_LOCATION}/"*"$1"*.dump | head -n1 )
    # aborts here (thanks to set+e) if `ls` doesn't find a matching expansion: "ls: cannot access ${LATEST_DUMP_LOCATION}/*XXXXXX*.dump: No such file or directory"
else
    echo "looking for most recent dump .."
    DUMP_TO_LOAD=$( ls -dt "${LATEST_DUMP_LOCATION}"/*.dump | head -n1 )
fi
echo " .. found '${DUMP_TO_LOAD}'"
du -hs "${DUMP_TO_LOAD}"

# Work around pg_restore failing due to an option set automatically by Postgres clients >= 9.3
echo "Loading dump..."
time pg_restore --username=postgres --host=localhost --port=$PORT --dbname=otp --jobs=$PRODUCTION_POSTGRES_JOB_COUNT --no-privileges "${DUMP_TO_LOAD}" || true

echo "Dump loaded"

PSQL="psql --username=otp --host=localhost --port=$PORT --dbname=otp"
echo "Disabling workflows plans"
${PSQL} --command "update job_execution_plan set enabled='f' where enabled='t';"
echo "Marking all not finished workflows as finished"
${PSQL} --command "UPDATE public.process SET finished='true' WHERE finished='false';"
echo "Marking all cluster jobs as finished"
${PSQL} --command "UPDATE public.cluster_job SET check_status = 'FINISHED' WHERE check_status != 'FINISHED';"

echo "Disabling workflow runs"
${PSQL} --command "UPDATE public.workflow_run SET state = 'FAILED' WHERE state = 'PENDING';"
echo "Disabling workflow steps"
${PSQL} --command "UPDATE public.workflow_step SET state = 'FAILED' WHERE state = 'CREATED';"

echo ""
