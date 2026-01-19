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
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 0, 'Default resources values for Roddy ACEseq (CNV calling) 1.2.8-4 WHOLE_GENOME PAIRED',
        '{' ||
        '    "RODDY": {' ||
        '        "resources": {' ||
        '            "estimatePurityPloidy": {' ||
        '                "memory": "2",' ||
        '                "value": "purity_ploidy_estimation_final.R",' ||
        '                "nodes": 1,' ||
        '                "walltime": "5",' ||
        '                "cores": 2,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "generatePlots": {' ||
        '                "memory": "50",' ||
        '                "value": "pscbs_plots.R",' ||
        '                "nodes": 1,' ||
        '                "walltime": "12",' ||
        '                "cores": 1,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "cnvSnpGeneration": {' ||
        '                "memory": "2",' ||
        '                "value": "cnv_snvMpileup.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "30",' ||
        '                "cores": 2,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "annotateCnvFiles": {' ||
        '                "memory": "0.2",' ||
        '                "value": "vcfAnno.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "6",' ||
        '                "cores": 2,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "replaceBadControl": {' ||
        '                "memory": "0.2",' ||
        '                "value": "replaceControl.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "5",' ||
        '                "cores": 2,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "mergeAndFilterCnvFiles": {' ||
        '                "memory": "0.2",' ||
        '                "value": "cnvMergeFilter.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "5",' ||
        '                "cores": 2,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "mergeAndFilterCnvFiles_withReplaceBadControl": {' ||
        '                "memory": "0.2",' ||
        '                "value": "cnvMergeFilter.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "5",' ||
        '                "cores": 2,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "mergeAndFilterSnpFiles": {' ||
        '                "memory": "0.2",' ||
        '                "value": "snvMergeFilter.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "5",' ||
        '                "cores": 2,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "getGenotypes": {' ||
        '                "memory": "1",' ||
        '                "value": "estimateGenotypes.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "3",' ||
        '                "cores": 1,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "createUnphasedGenotype": {' ||
        '                "memory": "0.2",' ||
        '                "value": "createUnphasedFiles.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "5",' ||
        '                "cores": 1,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "imputeGenotypes_noMpileup": {' ||
        '                "memory": "10",' ||
        '                "value": "impute2.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "10",' ||
        '                "cores": 1,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "imputeGenotypes_X_noMpileup": {' ||
        '                "memory": "10",' ||
        '                "value": "impute2_X.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "12",' ||
        '                "cores": 1,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "imputeGenotypes": {' ||
        '                "memory": "10",' ||
        '                "value": "impute2.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "20",' ||
        '                "cores": 1,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "imputeGenotypes_X": {' ||
        '                "memory": "5",' ||
        '                "value": "impute2_X.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "10",' ||
        '                "cores": 1,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "addHaplotypesToSnpFile": {' ||
        '                "memory": "0.2",' ||
        '                "value": "haplotypes.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "10",' ||
        '                "cores": 2,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "createControlBafPlots": {' ||
        '                "memory": "5",' ||
        '                "value": "createControlBafPlots.sh",' ||
        '                "walltime": "1",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "correctGcBias": {' ||
        '                "memory": "2",' ||
        '                "value": "correct_gc_bias.sh",' ||
        '                "walltime": "1",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "getBreakpoints": {' ||
        '                "memory": "30",' ||
        '                "value": "datatablePSCBSgaps.sh",' ||
        '                "walltime": "5",' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "mergeBreakpointsAndSvDelly": {' ||
        '                "memory": "0.2",' ||
        '                "value": "PSCBSgaps_Delly.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "2",' ||
        '                "cores": 2,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "mergeBreakpointsAndSvCrest": {' ||
        '                "memory": "0.2",' ||
        '                "value": "mergePSCBSCrest.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "1",' ||
        '                "cores": 2,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "getSegmentsAndSnps": {' ||
        '                "memory": "25",' ||
        '                "value": "PSCBSall.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "20",' ||
        '                "cores": 2,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "markHomozygousDeletions": {' ||
        '                "memory": "2",' ||
        '                "value": "homozygDel.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "1",' ||
        '                "cores": 1,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "segmentsToSnpDataHomodel": {' ||
        '                "memory": "1",' ||
        '                "value": "segmentsDataHomoDel.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "4",' ||
        '                "cores": 2,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "clusterAndPruneSegments": {' ||
        '                "memory": "20",' ||
        '                "value": "clusteredPrunedNormal.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "160",' ||
        '                "cores": 1,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "segmentsToSnpDataPruned": {' ||
        '                "memory": "1",' ||
        '                "value": "segmentsPrunedNormal.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "2",' ||
        '                "cores": 2,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "estimatePeaksForPurity": {' ||
        '                "memory": "12",' ||
        '                "value": "purityPloidity.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "10",' ||
        '                "cores": 1,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "estimatePurityAndPloidy": {' ||
        '                "memory": "7",' ||
        '                "value": "purityPloidity_EstimateFinal.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "5",' ||
        '                "cores": 1,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "generateResultsAndPlots": {' ||
        '                "memory": "30",' ||
        '                "value": "plots.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "35",' ||
        '                "cores": 1,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            },' ||
        '            "generateVcfFromTab": {' ||
        '                "memory": "1",' ||
        '                "value": "convertTabTovcf.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "2",' ||
        '                "cores": 1,' ||
        '                "basepath": "copyNumberEstimationWorkflow"' ||
        '            }' ||
        '        }' ||
        '    }' ||
        '}')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 'Default resources values for Roddy ACEseq (CNV calling) 1.2.8-4 WHOLE_GENOME PAIRED', 22,
        'DEFAULT_VALUES',
        (SELECT id FROM external_workflow_config_fragment WHERE name = 'Default resources values for Roddy ACEseq (CNV calling) 1.2.8-4 WHOLE_GENOME PAIRED'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for Roddy ACEseq (CNV calling) 1.2.8-4 WHOLE_GENOME PAIRED'),
       (SELECT id FROM workflow WHERE name = 'Roddy ACEseq (CNV calling)')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for Roddy ACEseq (CNV calling) 1.2.8-4 WHOLE_GENOME PAIRED'),
       (SELECT id
        FROM workflow_version
        WHERE api_version_id = (SELECT id FROM workflow_api_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy ACEseq (CNV calling)'))
          AND workflow_version.workflow_version = '1.2.8-4')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for Roddy ACEseq (CNV calling) 1.2.8-4 WHOLE_GENOME PAIRED'),
       (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME' AND single_cell = FALSE AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;
