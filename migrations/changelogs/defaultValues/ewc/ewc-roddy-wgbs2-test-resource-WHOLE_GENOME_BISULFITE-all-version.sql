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
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 0, 'Default test-resource values for WGBS alignment WHOLE_GENOME_BISULFITE test',
        '{' ||
        '    "RODDY": {' ||
        '        "resources": {' ||
        '            "alignAndPairSlim": {' ||
        '                "memory": "50",' ||
        '                "value": "bwaMemSortSlimWithReadConversionForBisulfiteData.sh",' ||
        '                "walltime": "1",' ||
        '                "basepath": "bisulfiteWorkflow"' ||
        '            }' ||
        '        }' ||
        '    }' ||
        '}')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 'Default test-resource values for WGBS alignment WHOLE_GENOME_BISULFITE test', 100, 'DEFAULT_VALUES', (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default test-resource values for WGBS alignment WHOLE_GENOME_BISULFITE test'
                                                       AND deprecation_date IS NULL))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default test-resource values for WGBS alignment WHOLE_GENOME_BISULFITE test'),
       (SELECT id FROM workflow WHERE name = 'WGBS alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default test-resource values for WGBS alignment WHOLE_GENOME_BISULFITE test'),
       (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME_BISULFITE' AND single_cell = FALSE AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;
