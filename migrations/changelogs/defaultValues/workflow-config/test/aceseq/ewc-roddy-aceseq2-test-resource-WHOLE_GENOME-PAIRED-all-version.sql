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
 */
INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 0, 'Default test-resource values for Roddy ACEseq (CNV calling) WHOLE_GENOME PAIRED test',
        '{' ||
        '    "RODDY": {' ||
        '        "resources": {' ||
        '            "estimatePurityPloidy": {' ||
        '                "value": "purity_ploidy_estimation_final.R",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "generatePlots": {' ||
        '                "value": "pscbs_plots.R",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "cnvSnpGeneration": {' ||
        '                "value": "cnv_snvMpileup.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "annotateCnvFiles": {' ||
        '                "value": "vcfAnno.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "replaceBadControl": {' ||
        '                "value": "replaceControl.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "mergeAndFilterCnvFiles": {' ||
        '                "value": "cnvMergeFilter.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "mergeAndFilterCnvFiles_withReplaceBadControl": {' ||
        '                "value": "cnvMergeFilter.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "mergeAndFilterSnpFiles": {' ||
        '                "value": "snvMergeFilter.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "getGenotypes": {' ||
        '                "value": "estimateGenotypes.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "createUnphasedGenotype": {' ||
        '                "value": "createUnphasedFiles.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "imputeGenotypes_noMpileup": {' ||
        '                "value": "impute2.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "imputeGenotypes_X_noMpileup": {' ||
        '                "value": "impute2_X.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "imputeGenotypes": {' ||
        '                "value": "impute2.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "imputeGenotypes_X": {' ||
        '                "value": "impute2_X.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "addHaplotypesToSnpFile": {' ||
        '                "value": "haplotypes.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "createControlBafPlots": {' ||
        '                "value": "createControlBafPlots.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "correctGcBias": {' ||
        '                "value": "correct_gc_bias.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "getBreakpoints": {' ||
        '                "value": "datatablePSCBSgaps.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "mergeBreakpointsAndSvDelly": {' ||
        '                "value": "PSCBSgaps_Delly.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "mergeBreakpointsAndSvCrest": {' ||
        '                "value": "mergePSCBSCrest.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "getSegmentsAndSnps": {' ||
        '                "value": "PSCBSall.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "markHomozygousDeletions": {' ||
        '                "value": "homozygDel.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "segmentsToSnpDataHomodel": {' ||
        '                "value": "segmentsDataHomoDel.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "clusterAndPruneSegments": {' ||
        '                "value": "clusteredPrunedNormal.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "segmentsToSnpDataPruned": {' ||
        '                "value": "segmentsPrunedNormal.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "estimatePeaksForPurity": {' ||
        '                "value": "purityPloidity.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "estimatePurityAndPloidy": {' ||
        '                "value": "purityPloidity_EstimateFinal.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "generateResultsAndPlots": {' ||
        '                "value": "plots.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "generateVcfFromTab": {' ||
        '                "value": "convertTabTovcf.sh",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            }' ||
        '        }' ||
        '    }' ||
        '}')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 'Default test-resource values for Roddy ACEseq (CNV calling) WHOLE_GENOME PAIRED test', 100,
        'DEFAULT_VALUES',
        (SELECT id FROM external_workflow_config_fragment WHERE name = 'Default test-resource values for Roddy ACEseq (CNV calling) WHOLE_GENOME PAIRED test'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default test-resource values for Roddy ACEseq (CNV calling) WHOLE_GENOME PAIRED test'),
       (SELECT id FROM workflow WHERE name = 'Roddy ACEseq (CNV calling)')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default test-resource values for Roddy ACEseq (CNV calling) WHOLE_GENOME PAIRED test'),
       (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME' AND single_cell = FALSE AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;
