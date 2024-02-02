/*
 * Copyright 2011-2024 The OTP authors
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

    String searchString = 'Default cvalue values for RNA alignment%'

    changeSet(author: "-", id: "otp-2246") {
        sql("""
DELETE
FROM external_workflow_config_selector_workflow
WHERE external_workflow_config_selector_workflows_id IN (
    SELECT id
    FROM external_workflow_config_selector
    WHERE name LIKE '${searchString}'
);

DELETE
FROM external_workflow_config_selector_workflow_version
WHERE external_workflow_config_selector_workflow_versions_id IN (
    SELECT id
    FROM external_workflow_config_selector
    WHERE name LIKE '${searchString}'
);

DELETE
FROM external_workflow_config_selector_seq_type
WHERE external_workflow_config_selector_seq_types_id IN (
    SELECT id
    FROM external_workflow_config_selector
    WHERE name LIKE '${searchString}'
);

DELETE
FROM external_workflow_config_selector
WHERE name LIKE '${searchString}';

DELETE
FROM external_workflow_config_fragment
WHERE name LIKE '${searchString}';

""")
    }
}
