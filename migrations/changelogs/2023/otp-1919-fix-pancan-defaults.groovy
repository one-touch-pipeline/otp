/*
 * Copyright 2011-2023 The OTP authors
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

    changeSet(author: "", id: "") {
        sql("""
DELETE
FROM workflow_version_selector
where workflow_version_id IN (SELECT id
                              FROM workflow_version
                              WHERE workflow_id IN (SELECT id
                                                    FROM workflow
                                                    WHERE name = 'PanCancer alignment')
                                AND workflow_version.workflow_version IN ('1.2.73-2', '1.2.73-3'));

DELETE
FROM workflow_version
WHERE workflow_id IN (SELECT id
                      FROM workflow
                      WHERE name = 'PanCancer alignment')
  AND workflow_version.workflow_version IN ('1.2.73-2', '1.2.73-3');
 
DELETE
FROM workflow_version
WHERE workflow_id IN (SELECT id
                      FROM workflow
                      WHERE name = 'WGBS alignment')
  AND workflow_version.workflow_version IN ('1.2.51-2', '1.2.73-3');
""")
    }
}
