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

INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES (nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default cvalue values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1 RNA PAIRED',
        '{' ||
        '    "RODDY": {' ||
        '        "cvalues": {' ||
        '            "useSingleEndProcessing": {' ||
        '                "value": "false",' ||
        '                "type": "boolean"' ||
        '            }' ||
        '        }' ||
        '    }' ||
        '}') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
VALUES (nextval('hibernate_sequence'), 0, now(), now(), 'Default cvalue values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1 RNA PAIRED', 22,
        'DEFAULT_VALUES', (
            SELECT id
            FROM external_workflow_config_fragment
            WHERE name = 'Default cvalue values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1 RNA PAIRED')) ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1 RNA PAIRED'),
       (SELECT id FROM workflow WHERE name = 'RNA alignment') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1 RNA PAIRED'),
       (SELECT id
        FROM workflow_version
        WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'RNA alignment') AND workflow_version.workflow_version = '1.2.22-6') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1 RNA PAIRED'),
       (SELECT id
        FROM workflow_version
        WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'RNA alignment') AND workflow_version.workflow_version = '1.2.22-7') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1 RNA PAIRED'),
       (SELECT id
        FROM workflow_version
        WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'RNA alignment') AND workflow_version.workflow_version = '1.3.0') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1 RNA PAIRED'),
       (SELECT id
        FROM workflow_version
        WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'RNA alignment') AND workflow_version.workflow_version = '1.3.0-1') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1 RNA PAIRED'),
       (SELECT id FROM seq_type WHERE name = 'RNA' AND single_cell = FALSE AND library_layout = 'PAIRED') ON CONFLICT DO NOTHING;
