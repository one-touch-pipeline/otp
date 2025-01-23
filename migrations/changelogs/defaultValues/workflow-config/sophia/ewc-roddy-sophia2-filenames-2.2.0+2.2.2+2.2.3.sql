/*
 * Copyright 2011-2025 The OTP authors
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
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default filenames values for Roddy Sophia (structural variation calling) 2.2.0, 2.2.2, 2.2.3',
'{' ||
'    "RODDY_FILENAMES": {' ||
'        "filenames": [' ||
'            {' ||
'                "class": "SophiaOutputBedpe",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/${sample}_${p'||'id}_bps.tsv.gz",' ||
'                "derivedFrom": "BasicBamFile"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredBedpeFileNoControl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-only_filtered.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredBedpeFileNoControlPdf",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-only_filtered.pdf",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredDedupBedpeFileNoControl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-only_filtered_dedup.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredSomaticBedpeFileNoControlACEseq",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_filtered_minEventScore3.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredQJsonNoControl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-only.qualitycontrol.json",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredBedpeFileTmVsCntrl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-${controlSample}_filtered.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredDedupBedpeFileTmVsCntrl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-${controlSample}_filtered_dedup.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredPdfFileTmVsCntrl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-${controlSample}_filtered.tsv_score_${circlizeScoreThreshold}_scaled_merged.pdf",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredGermlineBedpeFileTmVsCntrl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-${controlSample}_filtered_germlineStrict.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredDedupGermlineBedpeFileTmVsCntrl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-${controlSample}_filtered_dedup_germlineStrict.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredSomaticBedpeFileTmVsCntrl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-${controlSample}_filtered_somatic.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredDedupSomaticBedpeFileTmVsCntrl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-${controlSample}_filtered_dedup_somatic.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredSomaticBedpeFileTmVsCntrlACEseq",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_filtered_somatic_minEventScore3.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredQJsonTmVsCntrl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-${controlSample}.qualitycontrol.json",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredSomaticBedpeFileTmVsCntrlACEseqOverhangCandidates",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-${controlSample}_filtered_somatic_overhangCandidates.tsv",' ||
'                "onScriptParameter": "BEDPE_RESULT_FILE_FILTERED_SOMATIC_OVERHANG_CANDIDATES"' ||
'            },' ||
'            {' ||
'                "class": "InsertSizesValueFile",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${sample}/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png_qcValues.txt",' ||
'                "derivedFrom": "BasicBamFile"' ||
'            },' ||
'            {' ||
'                "class": "SophiaOutputBedpe",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/${sample}_${p'||'id}_bps.tsv.gz",' ||
'                "derivedFrom": "BasicBamFile"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredGermlineBedpeFileNoControl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-only_filtered_germlineStrict.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredBedpeFileNoControl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-only_filtered.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredSomaticBedpeFileNoControl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-only_filtered_somatic.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredSomaticBedpeFileNoControlACEseq",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_filtered_somatic_minEventScore3.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredQJsonNoControl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-only.qualitycontrol.json",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredGermlineBedpeFileTmVsCntrl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-${controlSample}_filtered_germlineStrict.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredBedpeFileTmVsCntrl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-${controlSample}_filtered.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredSomaticBedpeFileTmVsCntrl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_${tumorSample}-${controlSample}_filtered_somatic.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredSomaticBedpeFileTmVsCntrlACEseq",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/svs_${p'||'id}_filtered_somatic_minEventScore3.tsv",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            },' ||
'            {' ||
'                "class": "AnnotationFilteredQJsonTmVsCntrl",' ||
'                "pattern": "${outputAnalysisBaseDirectory}/${sophiaOutputDirectory}/qualitycontrol.json",' ||
'                "derivedFrom": "SophiaOutputBedpe"' ||
'            }' ||
'        ]' ||
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default filenames values for Roddy Sophia (structural variation calling) 2.2.0, 2.2.2, 2.2.3', 6, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default filenames values for Roddy Sophia (structural variation calling) 2.2.0, 2.2.2, 2.2.3'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for Roddy Sophia (structural variation calling) 2.2.0, 2.2.2, 2.2.3'), (SELECT id FROM workflow WHERE name = 'Roddy Sophia (structural variation calling)')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for Roddy Sophia (structural variation calling) 2.2.0, 2.2.2, 2.2.3'), (SELECT id FROM workflow_version WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Sophia (structural variation calling)')) AND workflow_version.workflow_version = '2.2.0')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for Roddy Sophia (structural variation calling) 2.2.0, 2.2.2, 2.2.3'), (SELECT id FROM workflow_version WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Sophia (structural variation calling)')) AND workflow_version.workflow_version = '2.2.2')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default filenames values for Roddy Sophia (structural variation calling) 2.2.0, 2.2.2, 2.2.3'), (SELECT id FROM workflow_version WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Sophia (structural variation calling)')) AND workflow_version.workflow_version = '2.2.3')
    ON CONFLICT DO NOTHING;
