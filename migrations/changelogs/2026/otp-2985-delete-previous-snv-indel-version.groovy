/*
 * Copyright 2011-2026 The OTP authors
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

    changeSet(author: "-", id: "otp-2985") {
        preConditions(onFail: "MARK_RAN") {
            sqlCheck(expectedResult: "2", """
                SELECT COUNT(*) FROM workflow_version wv
                JOIN workflow_api_version wav ON wv.api_version_id = wav.id
                JOIN workflow w ON wav.workflow_id = w.id
                WHERE (wv.workflow_version = '1.2.166-6' AND w.name = 'Roddy SNV calling')
                   OR (wv.workflow_version = '1.2.177-603' AND w.name = 'Roddy Indel calling')
            """)
        }

        sql("""
INSERT INTO workflow_api_version(id, date_created, last_updated, version, workflow_id, identifier)
VALUES (NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, (SELECT id FROM workflow WHERE name = 'Roddy SNV calling'), 2)
ON CONFLICT DO NOTHING;

INSERT INTO workflow_api_version(id, date_created, last_updated, version, workflow_id, identifier)
VALUES (NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, (SELECT id FROM workflow WHERE name = 'Roddy Indel calling'), 2)
ON CONFLICT DO NOTHING;

UPDATE workflow_version
SET api_version_id = (
    SELECT id FROM workflow_api_version
    WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy SNV calling')
      AND identifier = 2
)
WHERE workflow_version = '1.2.166-6'
  AND api_version_id IN (
    SELECT id FROM workflow_api_version
    WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy SNV calling')
  );

UPDATE workflow_version
SET api_version_id = (
    SELECT id FROM workflow_api_version
    WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Indel calling')
      AND identifier = 2
)
WHERE workflow_version = '1.2.177-603'
  AND api_version_id IN (
    SELECT id FROM workflow_api_version
    WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Indel calling')
  );
        """)
    }
}
