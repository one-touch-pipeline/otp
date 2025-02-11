/*
 * Copyright 2011-2025 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

databaseChangeLog = {
    changeSet(author: "-", id: "otp-2700-1") {
        sql("""
UPDATE workflow_version
SET workflow_version = REPLACE(workflow_version, 'cellranger', 'CellRanger')
WHERE api_version_id =
      (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id =
            (SELECT id FROM workflow WHERE name = 'Cell Ranger'));
""")
    }

    changeSet(author: "-", id: "otp-2700-2") {
        sql("""
UPDATE processing_option
SET value = REPLACE(value, 'cellranger', 'CellRanger')
WHERE name IN ('PIPELINE_CELLRANGER_DEFAULT_VERSION', 'PIPELINE_CELLRANGER_AVAILABLE_VERSIONS')
""")
    }

    changeSet(author: "-", id: "otp-2700-3") {
        sql("""
UPDATE config_per_project_and_seq_type
SET program_version = REPLACE(program_version, 'cellranger', 'CellRanger')
WHERE class = 'de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfig';
""")
    }
}
