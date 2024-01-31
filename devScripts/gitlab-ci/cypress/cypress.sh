#!/bin/sh
#
# Copyright 2011-2022 The OTP authors
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

set -ev -o pipefail

cp $DOCKER_ENV ./.env
cp $CYPRESS_ENV ./cypress.env.json
cp $OTP_PROPERTIES_CYPRESS ~/.otp.properties

echo "===================================="
docker compose -f docker-compose.yml up --build postgres > logs/postgres.log &
docker compose -f docker-compose.yml up --build open-ldap > logs/open-ldap.log &
docker compose -f docker-compose.yml up --build openssh-server > logs/openssh-server.log &

# precompile classes and install npm dependencies, so npm_run_cy-wait doesn't need to wait for building, since wait time is limited
echo "===================================="
./gradlew --build-cache classes npm_install_cypress

# wait for database to be ready and apply all database changes before starting of OTP, since bootrun already responses to request before
# the database migration has run through and if there are changes to tables used by 'http-get://localhost:8080' a lot of logs are created
timeout 90s bash -c 'until docker exec otp-dev-postgres pg_isready ; do sleep 5 ; done'
echo "===================================="
./gradlew --build-cache dbmUpdate

echo "===================================="
./gradlew --build-cache npm_run_cy-wait | tee cypressReport.txt
