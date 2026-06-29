#!/usr/bin/with-contenv bash
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

# Creates the WESkit report file for the OTP-2980 demo scenario.
#
# The WorkFolder UUID is fixed in CreateWesRestartScenarioData.groovy so this script
# can place the file at the correct path without querying the database.
# FilestoreService splits the UUID into 3 path segments: uuid[0:2]/uuid[2:4]/uuid[4:]
#
# UUID:  c0ffee00-2980-4000-b000-000000002980
# Path:  <baseFolder>/c0/ff/ee00-2980-4000-b000-000000002980/report/report-wes-restart-demo.html

set -ev

UUID="c0ffee00-2980-4000-b000-000000002980"
BASE_FOLDER="/home/otp/filesystem/otp_data"
REPORT_DIR="${BASE_FOLDER}/${UUID:0:2}/${UUID:2:2}/${UUID:4}/report"

mkdir -p "${REPORT_DIR}"

cat > "${REPORT_DIR}/report-wes-restart-demo.html" <<'EOF'
<!DOCTYPE html>
<html>
<head><title>WESkit Report — OTP-2980 Demo</title></head>
<body>
  <h1>WESkit Report (Restarted Step Demo)</h1>
  <p>This report belongs to the <strong>restarted</strong> WES execution step.</p>
  <p>The old (obsolete) step's report is no longer accessible after the fix for OTP-2980.</p>
</body>
</html>
EOF

chown -R otp:otp "${BASE_FOLDER}"
echo "OTP-2980: WES restart report file created at ${REPORT_DIR}"
