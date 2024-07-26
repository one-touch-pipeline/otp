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

INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 0, 'Default resources values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1',
        '{' ||
        '    "RODDY": {' ||
        '        "resources": {' ||
        '            "cleanupScript": {' ||
        '                "memory": "0.1",' ||
        '                "value": "cleanupScript.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "1",' ||
        '                "cores": 1,' ||
        '                "basepath": "rnaseqworkflow"' ||
        '            },' ||
        '            "starAlignment": {' ||
        '                "memory": "100g",' ||
        '                "value": "HIPO2_rnaseq_processing.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "360h",' ||
        '                "cores": 8,' ||
        '                "basepath": "rnaseqworkflow"' ||
        '            }' ||
        '        }' ||
        '    }' ||
        '}')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 'Default resources values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1', 6, 'DEFAULT_VALUES',
        (SELECT id
         FROM external_workflow_config_fragment
         WHERE name = 'Default resources values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1'),
       (SELECT id FROM workflow WHERE name = 'RNA alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id =
              (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'RNA alignment'))
          AND workflow_version.workflow_version = '1.2.22-6')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id =
              (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'RNA alignment'))
          AND workflow_version.workflow_version = '1.2.22-7')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id =
              (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'RNA alignment'))
          AND workflow_version.workflow_version = '1.3.0')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for RNA alignment 1.2.22-6, 1.2.22-7, 1.3.0, 1.3.0-1'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id =
              (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'RNA alignment'))
          AND workflow_version.workflow_version = '1.3.0-1')
ON CONFLICT DO NOTHING;
