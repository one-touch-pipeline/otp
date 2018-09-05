#!/bin/bash
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

# Wait a few seconds for the server to come up. Just in case.
sleep 10

LATEST_DUMP=`find ${LATEST_DUMP_LOCATION} -maxdepth 1 -name "*.dump" | sort | tail -n 1`
echo "Latest dump: $LATEST_DUMP"
du -hs ${LATEST_DUMP}

# Work around pg_restore failing due to an option set automatically by Postgres clients >= 9.3
echo "Loading dump..."
time pg_restore --username=postgres --host=localhost --dbname=otp --jobs=4 ${LATEST_DUMP} || true
echo "Dump loaded"

PSQL="psql --username=otp --host=localhost --dbname=otp"
echo "Disabling workflows plans"
${PSQL} --command "update job_execution_plan set enabled='f' where enabled='t';"
echo "Marking all not finished workflows as finished"
${PSQL} --command "UPDATE public.process SET finished='true' WHERE finished='false';"

echo ""
