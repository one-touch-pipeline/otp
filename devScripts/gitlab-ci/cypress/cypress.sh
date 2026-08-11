#!/bin/bash
#
# Copyright 2011-2026 The OTP authors
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

if [[ ! -v SUITE ]]
then
    echo "The variable SUITE is not defined"
    echo "Please define it with: SUITE='$value'"
    exit 1
fi

SUITE_DIR="cypress/e2e/${SUITE}"

if [[ ! -d ${SUITE_DIR} ]]
then
    echo "No cypress tests exist for suite '${SUITE} '"
    exit 2
fi

# the spec file pattern, provided via ENV
export spec="${SUITE_DIR}/**/*.spec.js"

# disable spring dev-tools reload
export DISABLE_RESTART=true

# copy files given via variable
cp $DOCKER_ENV ./.env
cp $OTP_PROPERTIES_CYPRESS ~/.otp.properties

# append repo-controlled Cypress-only properties (e.g. otp.testing.endpoints.enabled) so they don't
# have to live in the shared $OTP_PROPERTIES_CYPRESS CI variable
if [[ -f cypress/extra.cypress.properties ]]
then
    printf '\n' >> ~/.otp.properties
    cat cypress/extra.cypress.properties >> ~/.otp.properties
    echo "Appended cypress/extra.cypress.properties to ~/.otp.properties"
fi

cypressSecret="$(sed -n -E 's/^[[:space:]]*"otp\.autoimport\.secret"[[:space:]]*:[[:space:]]*"([^"]*)".*$/\1/p' cypress/cypress.env.json | tail -n 1)"

if [[ -z "$cypressSecret" ]]
then
    echo "otp.autoimport.secret is missing or empty in cypress/cypress.env.json"
    exit 1
fi
printf '\notp.autoimport.secret=%s\n' "$cypressSecret" >> ~/.otp.properties
echo "Configured otp.autoimport.secret for Cypress"

# create info about gitlab
docker info > logs/docker-info.log

echo "===================================="
docker compose -f docker-compose.yml up --no-build --quiet-pull postgres > logs/postgres.log 2>&1 &
(
  sleep 10s
  docker compose -f docker-compose.yml up --no-build --quiet-pull open-ldap > logs/open-ldap.log 2>&1 &
  docker compose -f docker-compose.yml up --no-build --quiet-pull openssh-server > logs/openssh-server.log 2>&1 &
)&

# wait for database to be ready and apply all database changes before starting of OTP, since bootrun already responses to request before
# the database migration has run through and if there are changes to tables used by 'http-get://localhost:8080' a lot of logs are created
# install npm & cypress dependencies, so npm_run_cy-wait doesn't need to wait for building, since wait time is limited
timeout 90s bash -c 'until docker exec otp-dev-postgres pg_isready ; do sleep 5 ; done'

echo "===================================="
./gradlew --build-cache dbmUpdate npm_exec_cypress_install

echo "===================================="
# wait for the services OTP needs at runtime, they had time to start up during the gradle build above

# slapd must listen on 389 and the bootstrap ldif must be applied. Anonymous reads are denied by the osixia ACLs,
# so the data is queried via the local socket, where root is authorized without a password.
timeout 60s bash -c 'until docker exec otp-dev-ldap ldapwhoami -x -H ldap://localhost:389 > /dev/null 2>&1 \
        && docker exec otp-dev-ldap ldapsearch -Y EXTERNAL -Q -H ldapi:/// -LLL -s base -b "ou=users,dc=otpldap,dc=dev" dn > /dev/null 2>&1
    do sleep 5 ; done'

# the openssh-server image starts sshd only after '/custom-cont-init.d' has extracted the test file system,
# so an sshd answering on 2222 also means the tarballs are unpacked
timeout 60s bash -c 'until docker exec otp-dev-ssh-server sh -c "nc -w 3 localhost 2222 < /dev/null | grep -q ^SSH-" 2> /dev/null ; do sleep 5 ; done'

echo "===================================="
./gradlew --build-cache npm_run_cy-wait | tee cypressReport.txt
