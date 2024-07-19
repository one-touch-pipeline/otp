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
VALUES (nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default test-resource values for Roddy SNV calling test',
        '{' ||
        '    "RODDY": {' ||
        '        "resources": {' ||
        '            "snvCallingMetaScript": {' ||
        '                "value": "snvCallingMetaScript.sh",' ||
        '                "walltime": "8",' ||
        '                "basepath": "snvPipeline"' ||
        '            },' ||
        '            "snvCalling": {' ||
        '                "memory": "1",' ||
        '                "value": "snvCalling.sh",' ||
        '                "walltime": "1",' ||
        '                "basepath": "snvPipeline"' ||
        '            },' ||
        '            "snvJoinVcfFiles": {' ||
        '                "memory": "1",' ||
        '                "value": "joinSNVVCFFiles.sh",' ||
        '                "basepath": "snvPipeline"' ||
        '            },' ||
        '            "cleanupScript": {' ||
        '                "value": "cleanupScript.sh",' ||
        '                "basepath": "snvPipeline"' ||
        '            }' ||
        '        }' ||
        '    }' ||
        '}') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
VALUES (nextval('hibernate_sequence'), 0, now(), now(), 'Default test-resource values for Roddy SNV calling test', 100, 'DEFAULT_VALUES',
        (SELECT id FROM external_workflow_config_fragment WHERE name = 'Default test-resource values for Roddy SNV calling test')) ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default test-resource values for Roddy SNV calling test'),
       (SELECT id FROM workflow WHERE name = 'Roddy SNV calling') ON CONFLICT DO NOTHING;
