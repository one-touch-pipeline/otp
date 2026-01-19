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
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default filenames values for Roddy Indel calling 2.0.0-101, 2.2.2, 2.4.1-1, 2.4.1-2',
'{' ||
'    "RODDY_FILENAMES": {' ||
'        "filenames": [' ||
'            {' ||
'                "class": "VCFFileForIndels",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/indel_${p'||'id}.vcf.raw.gz",' ||
'                "selectiontag": "rawVCF",' ||
'                "derivedFrom": "TumorBamFile"' ||
'            },' ||
'            {' ||
'                "class": "VCFFileForIndels",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/indel_${p'||'id}.vcf.gz",' ||
'                "selectiontag": "rawVCFConfidenceAnnotation",' ||
'                "derivedFrom": "TumorBamFile"' ||
'            },' ||
'            {' ||
'                "class": "VCFFileForIndels",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/indel_${p'||'id}.vcf.gz",' ||
'                "derivedFrom": "VCFFileForIndels"' ||
'            },' ||
'            {' ||
'                "class": "VCFFileForIndels",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/indel_${p'||'id}_somatic_indels_conf_${MIN_CONFIDENCE_SCORE}_to_10.vcf",' ||
'                "selectiontag": "somatic",' ||
'                "derivedFrom": "VCFFileForIndels"' ||
'            },' ||
'            {' ||
'                "class": "VCFFileForIndels",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/indel_${p'||'id}_somatic_functional_indels_conf_${MIN_CONFIDENCE_SCORE}_to_10.vcf",' ||
'                "selectiontag": "somatic_functional",' ||
'                "derivedFrom": "VCFFileForIndels"' ||
'            },' ||
'            {' ||
'                "class": "CPFileAnno",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/indelCallingCheckPoint_${TOOL_ID}",' ||
'                "onTool": "indelAnnotation"' ||
'            },' ||
'            {' ||
'                "class": "CPFileDeepAnno",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/indelCallingCheckPoint_${TOOL_ID}",' ||
'                "onTool": "indelDeepAnnotation"' ||
'            },' ||
'            {' ||
'                "class": "CPFileFilter",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/indelCallingCheckPoint_${TOOL_ID}",' ||
'                "onTool": "indelVcfFilter"' ||
'            },' ||
'            {' ||
'                "class": "CPFileAnno",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/indelCallingCheckPoint_${TOOL_ID}",' ||
'                "onTool": "indelAnnotationWithoutControl"' ||
'            },' ||
'            {' ||
'                "class": "CPFileFilter",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/indelCallingCheckPoint_${TOOL_ID}",' ||
'                "onTool": "indelVcfFilterWithoutControl"' ||
'            },' ||
'            {' ||
'                "class": "TextFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/indelCallingCheckPoint_${TOOL_ID}",' ||
'                "onScriptParameter": "FILENAME_CHECKPOINT_SWAP"' ||
'            },' ||
'            {' ||
'                "class": "VCFFileForIndels",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/smallVariants_${p'||'id}.rareGermline.vcf",' ||
'                "onScriptParameter": "FILENAME_RARE_GERMLINE"' ||
'            },' ||
'            {' ||
'                "class": "VCFFileForIndels",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/smallVariants_${p'||'id}.somaticRescue.vcf",' ||
'                "onScriptParameter": "FILENAME_SOMATIC_RESCUE"' ||
'            },' ||
'            {' ||
'                "class": "VCFFileForIndels",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/smallVariants_${p'||'id}.allSomatic.vcf",' ||
'                "onScriptParameter": "FILENAME_ALL_SOMATIC"' ||
'            },' ||
'            {' ||
'                "class": "VCFFileForIndels",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${mpileupPlatypusOutputDirectory}/checkSampleSwap.json",' ||
'                "onScriptParameter": "FILENAME_SWAP_JSON"' ||
'            }' ||
'        ]' ||
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default filenames values for Roddy Indel calling 2.0.0-101, 2.2.2, 2.4.1-1, 2.4.1-2', 6, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default filenames values for Roddy Indel calling 2.0.0-101, 2.2.2, 2.4.1-1, 2.4.1-2'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for Roddy Indel calling 2.0.0-101, 2.2.2, 2.4.1-1, 2.4.1-2'), (SELECT id FROM workflow WHERE name = 'Roddy Indel calling')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for Roddy Indel calling 2.0.0-101, 2.2.2, 2.4.1-1, 2.4.1-2'), (SELECT id FROM workflow_version WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Indel calling')) AND workflow_version.workflow_version = '2.0.0-101')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for Roddy Indel calling 2.0.0-101, 2.2.2, 2.4.1-1, 2.4.1-2'), (SELECT id FROM workflow_version WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Indel calling')) AND workflow_version.workflow_version = '2.2.2')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for Roddy Indel calling 2.0.0-101, 2.2.2, 2.4.1-1, 2.4.1-2'), (SELECT id FROM workflow_version WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Indel calling')) AND workflow_version.workflow_version = '2.4.1-1')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for Roddy Indel calling 2.0.0-101, 2.2.2, 2.4.1-1, 2.4.1-2'), (SELECT id FROM workflow_version WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Indel calling')) AND workflow_version.workflow_version = '2.4.1-2')
    ON CONFLICT DO NOTHING;
