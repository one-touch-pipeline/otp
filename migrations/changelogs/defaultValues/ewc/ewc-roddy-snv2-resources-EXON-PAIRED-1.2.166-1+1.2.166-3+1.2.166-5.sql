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
VALUES (nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default resources values for Roddy SNV calling 1.2.166-1, 1.2.166-3, 1.2.166-5 EXON PAIRED',
        '{' ||
        '    "RODDY": {' ||
        '        "resources": {' ||
        '            "snvCalling": {' ||
        '                "memory": "4",' ||
        '                "value": "snvCalling.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "48",' ||
        '                "cores": 1,' ||
        '                "basepath": "snvPipeline"' ||
        '            },' ||
        '            "snvAnnotation": {' ||
        '                "memory": "8",' ||
        '                "value": "snvAnnotation.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "180",' ||
        '                "cores": 2,' ||
        '                "basepath": "snvPipeline"' ||
        '            },' ||
        '            "snvDeepAnnotation": {' ||
        '                "memory": "4",' ||
        '                "value": "vcf_pipeAnnotator.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "4",' ||
        '                "cores": 3,' ||
        '                "basepath": "tools"' ||
        '            },' ||
        '            "snvFilter": {' ||
        '                "memory": "4",' ||
        '                "value": "filter_vcf.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "4",' ||
        '                "cores": 1,' ||
        '                "basepath": "snvPipeline"' ||
        '            }' ||
        '        }' ||
        '    }' ||
        '}') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
VALUES (nextval('hibernate_sequence'), 0, now(), now(), 'Default resources values for Roddy SNV calling 1.2.166-1, 1.2.166-3, 1.2.166-5 EXON PAIRED', 22,
        'DEFAULT_VALUES', (SELECT id
                           FROM external_workflow_config_fragment
                           WHERE name = 'Default resources values for Roddy SNV calling 1.2.166-1, 1.2.166-3, 1.2.166-5 EXON PAIRED')) ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default resources values for Roddy SNV calling 1.2.166-1, 1.2.166-3, 1.2.166-5 EXON PAIRED'),
       (SELECT id FROM workflow WHERE name = 'Roddy SNV calling') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default resources values for Roddy SNV calling 1.2.166-1, 1.2.166-3, 1.2.166-5 EXON PAIRED'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy SNV calling'))
          AND workflow_version.workflow_version = '1.2.166-1') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default resources values for Roddy SNV calling 1.2.166-1, 1.2.166-3, 1.2.166-5 EXON PAIRED'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy SNV calling'))
          AND workflow_version.workflow_version = '1.2.166-3') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default resources values for Roddy SNV calling 1.2.166-1, 1.2.166-3, 1.2.166-5 EXON PAIRED'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy SNV calling'))
          AND workflow_version.workflow_version = '1.2.166-5') ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default resources values for Roddy SNV calling 1.2.166-1, 1.2.166-3, 1.2.166-5 EXON PAIRED'),
       (SELECT id FROM seq_type WHERE name = 'EXON' AND single_cell = FALSE AND library_layout = 'PAIRED') ON CONFLICT DO NOTHING;
