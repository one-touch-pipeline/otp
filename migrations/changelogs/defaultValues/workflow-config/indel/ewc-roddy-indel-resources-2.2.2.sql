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
 *
 */

    INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default resources values for Roddy Indel calling 2.2.2',
'{' ||
'    "RODDY": {' ||
'        "resources": {' ||
'            "checkSampleSwap": {' ||
'                "memory": "10",' ||
'                "value": "checkSampleSwap_TiN.sh",' ||
'                "nodes": 1,' ||
'                "walltime": "24",' ||
'                "cores": 3,' ||
'                "basepath": "indelCallingWorkflow"' ||
'            },' ||
'            "indelDeepAnnotation": {' ||
'                "memory": "8",' ||
'                "value": "vcf_pipeAnnotator.sh",' ||
'                "nodes": 1,' ||
'                "walltime": "10",' ||
'                "cores": 2,' ||
'                "basepath": "indelCallingWorkflow"' ||
'            },' ||
'            "indelCallingWithoutControl": {' ||
'                "memory": "30",' ||
'                "value": "indelCalling.sh",' ||
'                "nodes": 1,' ||
'                "walltime": "110",' ||
'                "cores": 8,' ||
'                "basepath": "indelCallingWorkflow"' ||
'            },' ||
'            "indelAnnotationWithoutControl": {' ||
'                "memory": "8",' ||
'                "value": "platypusIndelAnnotation.sh",' ||
'                "nodes": 1,' ||
'                "walltime": "12",' ||
'                "cores": 3,' ||
'                "basepath": "indelCallingWorkflow"' ||
'            },' ||
'            "indelVcfFilterWithoutControl": {' ||
'                "memory": "5",' ||
'                "value": "filter_vcf.sh",' ||
'                "nodes": 1,' ||
'                "walltime": "12",' ||
'                "cores": 1,' ||
'                "basepath": "indelCallingWorkflow"' ||
'            }' ||
'        }' ||
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default resources values for Roddy Indel calling 2.2.2', 6, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default resources values for Roddy Indel calling 2.2.2'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for Roddy Indel calling 2.2.2'), (SELECT id FROM workflow WHERE name = 'Roddy Indel calling')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for Roddy Indel calling 2.2.2'), (SELECT id FROM workflow_version WHERE api_version_id in (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Indel calling')) AND workflow_version.workflow_version = '2.2.2')
    ON CONFLICT DO NOTHING;
