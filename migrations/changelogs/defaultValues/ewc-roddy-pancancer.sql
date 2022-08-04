/*
 * Copyright 2011-2021 The OTP authors
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
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0,'Default values for PanCancer workflow',
               '{' ||
               '    "RODDY": {' ||
               '        "cvalues": {' ||
               '            "useAcceleratedHardware": {' ||
               '                "value": "false",' ||
               '                "type": "boolean"' ||
               '            }' ||
               '        }' ||
               '    }' ||
               '}')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, selector_type, priority, external_workflow_config_fragment_id)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default values for PanCancer workflow', 'DEFAULT_VALUES',2, (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default values for PanCancer workflow'
))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer workflow'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
ON CONFLICT DO NOTHING;

--



INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default values for PanCancer alignment 1.2.73-1, 1.2.73-3, 1.2.73-201, 1.2.73-2 EXON',
       '{' ||
       '    "RODDY": {' ||
       '        "cvalues": {' ||
       '            "workflowEnvironmentScript": {' ||
       '                "value": "workflowEnvironment_tbiLsf",' ||
       '                "type": "string"' ||
       '            }' ||
       '        },' ||
       '        "resources": {' ||
       '            "cleanupScript": {' ||
       '                "memory": "0.1",' ||
       '                "value": "cleanupScript.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "1",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "fastqc": {' ||
       '                "value": "checkFastQC.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignment": {' ||
       '                "memory": "8",' ||
       '                "value": "bwaAlignSequence.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesort": {' ||
       '                "memory": "75",' ||
       '                "value": "bwaSampeSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesortSlim": {' ||
       '                "memory": "35",' ||
       '                "value": "bwaSampeSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPair": {' ||
       '                "memory": "20",' ||
       '                "value": "bwaMemSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "50",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPairSlim": {' ||
       '                "memory": "45",' ||
       '                "value": "bwaMemSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "180",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsIndex": {' ||
       '                "memory": "1",' ||
       '                "value": "samtoolsIndexBamfile.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "collectBamMetrics": {' ||
       '                "memory": "3",' ||
       '                "value": "picardCollectMetrics.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsFlagstat": {' ||
       '                "value": "samtoolsFlagstatBamfile.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "insertSizes": {' ||
       '                "value": "insertSizeDistribution.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "chromosomeDiff": {' ||
       '                "memory": "25",' ||
       '                "value": "differentiateChromosomes.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "genomeCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverage.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "readBinsCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverageReadBins.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlot": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlotSingle": {' ||
       '                "memory": "3",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates": {' ||
       '                "memory": "73",' ||
       '                "value": "mergeAndRemoveDuplicates.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimPicard": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimBioBambam": {' ||
       '                "memory": "15",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 3,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimSambamba": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "120",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "targetExtractCoverageSlim": {' ||
       '                "memory": "15",' ||
       '                "value": "targetExtractCoverageSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 2,' ||
       '                "basepath": "exomePipeline"' ||
       '            }' ||
       '        }' ||
       '    },' ||
       '    "RODDY_FILENAMES": {' ||
       '        ' ||
       '    }' ||
       '}'
      )
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, selector_type, priority, external_workflow_config_fragment_id)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default values for PanCancer alignment 1.2.73-1, 1.2.73-3, 1.2.73-201, 1.2.73-2 EXON', 'DEFAULT_VALUES', 22, (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default values for PanCancer alignment 1.2.73-1, 1.2.73-3, 1.2.73-201, 1.2.73-2 EXON'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-1, 1.2.73-3, 1.2.73-201, 1.2.73-2 EXON'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-1, 1.2.73-3, 1.2.73-201, 1.2.73-2 EXON'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-1')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-1, 1.2.73-3, 1.2.73-201, 1.2.73-2 EXON'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-3')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-1, 1.2.73-3, 1.2.73-201, 1.2.73-2 EXON'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-201')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-1, 1.2.73-3, 1.2.73-201, 1.2.73-2 EXON'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-2')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-1, 1.2.73-3, 1.2.73-201, 1.2.73-2 EXON'), (SELECT id FROM seq_type WHERE name = 'EXON' AND single_cell = false AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;


INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default values for PanCancer alignment 1.2.51-2, 1.2.51-1 WHOLE_GENOME',
       '{' ||
       '    "RODDY": {' ||
       '        "cvalues": {' ||
       '            "workflowEnvironmentScript": {' ||
       '                "value": "workflowEnvironment_tbiLsf",' ||
       '                "type": "string"' ||
       '            }' ||
       '        },' ||
       '        "resources": {' ||
       '            "cleanupScript": {' ||
       '                "memory": "0.1",' ||
       '                "value": "cleanupScript.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "1",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "fastqc": {' ||
       '                "value": "checkFastQC.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignment": {' ||
       '                "memory": "8",' ||
       '                "value": "bwaAlignSequence.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesort": {' ||
       '                "memory": "75",' ||
       '                "value": "bwaSampeSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesortSlim": {' ||
       '                "memory": "35",' ||
       '                "value": "bwaSampeSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPair": {' ||
       '                "memory": "20",' ||
       '                "value": "bwaMemSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "50",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPairSlim": {' ||
       '                "memory": "45",' ||
       '                "value": "bwaMemSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "160",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsIndex": {' ||
       '                "memory": "1",' ||
       '                "value": "samtoolsIndexBamfile.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "collectBamMetrics": {' ||
       '                "memory": "3",' ||
       '                "value": "picardCollectMetrics.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsFlagstat": {' ||
       '                "value": "samtoolsFlagstatBamfile.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "insertSizes": {' ||
       '                "value": "insertSizeDistribution.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "chromosomeDiff": {' ||
       '                "memory": "25",' ||
       '                "value": "differentiateChromosomes.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "genomeCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverage.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "readBinsCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverageReadBins.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlot": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlotSingle": {' ||
       '                "memory": "3",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates": {' ||
       '                "memory": "73",' ||
       '                "value": "mergeAndRemoveDuplicates.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimPicard": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimBioBambam": {' ||
       '                "memory": "15",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 3,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimSambamba": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            }' ||
       '        }' ||
       '    },' ||
       '    "RODDY_FILENAMES": {' ||
       '        ' ||
       '    }' ||
       '}'
      )
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, selector_type, priority, external_workflow_config_fragment_id)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default values for PanCancer alignment 1.2.51-2, 1.2.51-1 WHOLE_GENOME', 'DEFAULT_VALUES', 22, (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default values for PanCancer alignment 1.2.51-2, 1.2.51-1 WHOLE_GENOME'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.51-2, 1.2.51-1 WHOLE_GENOME'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.51-2, 1.2.51-1 WHOLE_GENOME'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.51-2')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.51-2, 1.2.51-1 WHOLE_GENOME'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.51-1')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.51-2, 1.2.51-1 WHOLE_GENOME'), (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME' AND single_cell = false AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;


INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default values for PanCancer alignment 1.2.73-2, 1.2.73-3, 1.2.73-201, 1.2.73-1 WHOLE_GENOME',
       '{' ||
       '    "RODDY": {' ||
       '        "cvalues": {' ||
       '            "workflowEnvironmentScript": {' ||
       '                "value": "workflowEnvironment_tbiLsf",' ||
       '                "type": "string"' ||
       '            }' ||
       '        },' ||
       '        "resources": {' ||
       '            "cleanupScript": {' ||
       '                "memory": "0.1",' ||
       '                "value": "cleanupScript.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "1",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "fastqc": {' ||
       '                "value": "checkFastQC.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignment": {' ||
       '                "memory": "8",' ||
       '                "value": "bwaAlignSequence.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesort": {' ||
       '                "memory": "75",' ||
       '                "value": "bwaSampeSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesortSlim": {' ||
       '                "memory": "35",' ||
       '                "value": "bwaSampeSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPair": {' ||
       '                "memory": "20",' ||
       '                "value": "bwaMemSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "50",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPairSlim": {' ||
       '                "memory": "45",' ||
       '                "value": "bwaMemSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "180",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsIndex": {' ||
       '                "memory": "1",' ||
       '                "value": "samtoolsIndexBamfile.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "collectBamMetrics": {' ||
       '                "memory": "3",' ||
       '                "value": "picardCollectMetrics.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsFlagstat": {' ||
       '                "value": "samtoolsFlagstatBamfile.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "insertSizes": {' ||
       '                "value": "insertSizeDistribution.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "chromosomeDiff": {' ||
       '                "memory": "25",' ||
       '                "value": "differentiateChromosomes.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "genomeCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverage.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "readBinsCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverageReadBins.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlot": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlotSingle": {' ||
       '                "memory": "3",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates": {' ||
       '                "memory": "73",' ||
       '                "value": "mergeAndRemoveDuplicates.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimPicard": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimBioBambam": {' ||
       '                "memory": "15",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 3,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimSambamba": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "120",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            }' ||
       '        }' ||
       '    },' ||
       '    "RODDY_FILENAMES": {' ||
       '        ' ||
       '    }' ||
       '}'
      )
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, selector_type, priority, external_workflow_config_fragment_id)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default values for PanCancer alignment 1.2.73-2, 1.2.73-3, 1.2.73-201, 1.2.73-1 WHOLE_GENOME', 'DEFAULT_VALUES', 22, (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default values for PanCancer alignment 1.2.73-2, 1.2.73-3, 1.2.73-201, 1.2.73-1 WHOLE_GENOME'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-2, 1.2.73-3, 1.2.73-201, 1.2.73-1 WHOLE_GENOME'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-2, 1.2.73-3, 1.2.73-201, 1.2.73-1 WHOLE_GENOME'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-2')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-2, 1.2.73-3, 1.2.73-201, 1.2.73-1 WHOLE_GENOME'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-3')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-2, 1.2.73-3, 1.2.73-201, 1.2.73-1 WHOLE_GENOME'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-201')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-2, 1.2.73-3, 1.2.73-201, 1.2.73-1 WHOLE_GENOME'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-1')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-2, 1.2.73-3, 1.2.73-201, 1.2.73-1 WHOLE_GENOME'), (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME' AND single_cell = false AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;


INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default values for PanCancer alignment WHOLE_GENOME',
       '{' ||
       '    "RODDY": {' ||
       '        "cvalues": {' ||
       '            "workflowEnvironmentScript": {' ||
       '                "value": "workflowEnvironment_tbiLsf",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "sampleDirectory": {' ||
       '                "value": "${inputBaseDirectory}/${pid}/${sample}/${SEQUENCER_PROTOCOL}",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "sequenceDirectory": {' ||
       '                "value": "${sampleDirectory}/${run}/sequence",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "outputAnalysisBaseDirectory": {' ||
       '                "value": "${outputBaseDirectory}",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "outputExecutionDirectory": {' ||
       '                "value": "${outputAnalysisBaseDirectory}/exec_${executionTimeString}"' ||
       '            },' ||
       '            "mergedBamSuffix_markDuplicates": {' ||
       '                "value": "merged.bam.dupmarked.bam",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergedBamSuffix_markDuplicatesShort": {' ||
       '                "value": "merged.mdup.bam",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergedBamSuffix_removeDuplicates": {' ||
       '                "value": "merged.bam.rmdup.bam",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergedBamSuffixList": {' ||
       '                "value": "${mergedBamSuffix_markDuplicates},${mergedBamSuffix_markDuplicatesShort},${mergedBamSuffix_removeDuplicates}",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "defaultMergedBamSuffix": {' ||
       '                "value": "${mergedBamSuffix_markDuplicatesShort}",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "pairedBamSuffix": {' ||
       '                "value": "paired.bam.sorted.bam",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "alignmentOutputDirectory": {' ||
       '                "value": "alignment"' ||
       '            },' ||
       '            "fastx_qcOutputDirectory": {' ||
       '                "value": "fastx_qc"' ||
       '            },' ||
       '            "coverageOutputDirectory": {' ||
       '                "value": "coverage"' ||
       '            },' ||
       '            "flagstatsOutputDirectory": {' ||
       '                "value": "flagstats"' ||
       '            },' ||
       '            "structuralVariationOutputDirectory": {' ||
       '                "value": "structural_variation"' ||
       '            },' ||
       '            "insertSizesOutputDirectory": {' ||
       '                "value": "insertsize_distribution"' ||
       '            },' ||
       '            "metricsOutputDirectory": {' ||
       '                "value": "metrics"' ||
       '            },' ||
       '            "mpileupOutputDirectory": {' ||
       '                "value": "mpileup"' ||
       '            },' ||
       '            "mpileupPlatypusOutputDirectory": {' ||
       '                "value": "platypus_indel"' ||
       '            },' ||
       '            "assembliesBaseDirectory": {' ||
       '                "value": "${sharedFilesBaseDirectory}/assemblies",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "assembliesHG191000GenomesDirectory": {' ||
       '                "value": "${assembliesBaseDirectory}/hg19_GRCh37_1000genomes",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "bwaIndexBaseDirectory_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/indexes/bwa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "bwaIndexBaseDirectory_methylCtools_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/indexes/methylCtools",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "referenceGenomeBaseDirectory_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/sequence",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesBaseDirectory_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/stats",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegionsBaseDirectory_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/targetRegions",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabasesDirectory": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/databases",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabaseUCSCDirectory": {' ||
       '                "value": "${hg19DatabasesDirectory}/UCSC",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabaseDBSNPDirectory": {' ||
       '                "value": "${hg19DatabasesDirectory}/dbSNP",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19Database1000GenomesDirectory": {' ||
       '                "value": "${hg19DatabasesDirectory}/1000genomes",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabaseIMPUTEDirectory": {' ||
       '                "value": "${hg19Database1000GenomesDirectory}/IMPUTE",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabaseENCODEDirectory": {' ||
       '                "value": "${hg19DatabasesDirectory}/ENCODE",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "bwaIndexBaseDirectory_mm10": {' ||
       '                "value": "${assembliesBaseDirectory}/mm10/indexes/bwa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "bwaIndexBaseDirectory_methylCtools_mm10": {' ||
       '                "value": "${assembliesBaseDirectory}/mm10/indexes/methylCtools",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesBaseDirectory_mm10": {' ||
       '                "value": "${assembliesBaseDirectory}/mm10/stats",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegionsBaseDirectory_mm10": {' ||
       '                "value": "${assembliesBaseDirectory}/mm10/targetRegions",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "meth_calls_converter_moabs": {' ||
       '                "value": "",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "indexPrefix_bwa05_1KGRef": {' ||
       '                "value": "${bwaIndexBaseDirectory_human}/bwa05_1KGRef/hs37d5.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa05_hg19_chr": {' ||
       '                "value": "${bwaIndexBaseDirectory_human}/bwa05_hg19_chr/hg19bwaidx",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_1KGRef": {' ||
       '                "value": "${bwaIndexBaseDirectory_human}/bwa06_1KGRef/hs37d5.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_hg19_chr": {' ||
       '                "value": "${bwaIndexBaseDirectory_human}/bwa06_hg19_chr/hg19_1-22_X_Y_M.fasta",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_mm10_GRC": {' ||
       '                "value": "${bwaIndexBaseDirectory_mm10}/bwa06/bwa06_GRCm38mm10/GRCm38mm10.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_mm10": {' ||
       '                "value": "${bwaIndexBaseDirectory_mm10}/bwa06/bwa06_mm10_UCSC/mm10_1-19_X_Y_M.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_methylCtools_mm10_GRC": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_GRCm38mm10/GRCm38mm10_PhiX_Lambda.conv.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_methylCtools_mm10_UCSC": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_mm10_UCSC/mm10_PhiX_Lambda.conv.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_methylCtools_1KGRef": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_human}/methylCtools_1KGRef/hs37d5_PhiX_Lambda.conv.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "ch_pos_index_methylCtools_1KGRef": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_human}/methylCtools_1KGRef/hs37d5_PhiX_Lambda.CG_CH.pos.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "ch_pos_index_methylCtools_mm10GRC": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_GRCm38mm10/GRCm38mm10_PhiX_Lambda.pos.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "ch_pos_index_methylCtools_mm10_UCSC": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_mm10_UCSC/mm10_PhiX_Lambda.pos.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "referenceGenome_1KGRef": {' ||
       '                "value": "${referenceGenomeBaseDirectory_human}/1KGRef/hs37d5.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "referenceGenome_hg19_chr": {' ||
       '                "value": "${referenceGenomeBaseDirectory_human}/hg19_chr/hg19_1-22_X_Y_M.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_hg19": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_human}/hg19_1-22_X_Y_M.fa.chrLenOnlyACGT.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_hs37": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_human}/hs37d5.fa.chrLenOnlyACGT_realChromosomes.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_mm10_GRC": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_mm10}/GRCm38mm10.fa.chrLenOnlyACGT_realChromosomes.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_mm10": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_mm10}/mm10_1-19_X_Y_M.fa.chrLenOnlyACGT_realChromosomes.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_hs37_bisulfite": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_human}/hs37d5_PhiX_Lambda.fa.chrLenOnlyACGT.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_mm10_GRC_bisulfite": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_mm10}/GRCm38mm10_PhiX_Lambda.fa.chrLenOnlyACGT.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_mm10_UCSC_bisulfite": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_mm10}/mm10_PhiX_Lambda.fa.chrLenOnlyACGT.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeLengthFile_hg19": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_human}/hg19_chrTotalLength.tsv",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent4withoutUTRs_chr": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent4withoutUTRs_chr.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent4withoutUTRs_plain": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent4withoutUTRs_plain.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent4withUTRs_plain": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent4withUTRs_plain.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent5withoutUTRs_chr": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withoutUTRs_chr.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent5withoutUTRs_plain": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withoutUTRs_plain.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent5withUTRs_chr": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withUTRs_chr.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent5withUTRs_plain": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withUTRs_plain.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "dbSNP_FILE": {' ||
       '                "value": "${hg19DatabasesDirectory}/dbSNP/dbSNP_135/00-All.SNV.vcf.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "SNP_REFERENCE": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/sequence/hg19_chr/hg19_1-22_X_Y_M.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "SNP_REFERENCE_ANNOTATIONS": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/Affymetrics/Affy5/chr/#CHROM#_AFFY.vcf",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "alignmentThreads": {' ||
       '                "value": "12",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "outputUMask": {' ||
       '                "value": "007",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "outputFileGroup": {' ||
       '                "value": "false"' ||
       '            },' ||
       '            "outputAccessRights": {' ||
       '                "value": "u+rw,g+rw,o-rwx"' ||
       '            },' ||
       '            "outputAccessRightsForDirectories": {' ||
       '                "value": "u+rwx,g+rwx,o-rwx"' ||
       '            },' ||
       '            "possibleControlSampleNamePrefixes": {' ||
       '                "value": "( blood BLOOD normal control CONTROL buffy_coat GERMLINE )",' ||
       '                "type": "bashArray"' ||
       '            },' ||
       '            "possibleTumorSampleNamePrefixes": {' ||
       '                "value": "( tumor TUMOR metastasis xenograft disease DISEASE relapse RELAPSE autopsy AUTOPSY metastasis METASTASIS )",' ||
       '                "type": "bashArray"' ||
       '            },' ||
       '            "useCentralAnalysisArchive": {' ||
       '                "value": "true"' ||
       '            },' ||
       '            "enableJobProfiling": {' ||
       '                "value": "false"' ||
       '            },' ||
       '            "JOB_PROFILER_BINARY": {' ||
       '                "value": "strace.sh"' ||
       '            },' ||
       '            "INDEX_PREFIX": {' ||
       '                "value": "${indexPrefix_bwa05_hg19_chr}",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "BWA_ALIGNMENT_OPTIONS": {' ||
       '                "value": "\"-q 20\""' ||
       '            },' ||
       '            "BWA_SAMPESORT_OPTIONS": {' ||
       '                "value": "\"-a 1000\""' ||
       '            },' ||
       '            "SAMPESORT_MEMSIZE": {' ||
       '                "value": 2000000000,' ||
       '                "type": "integer"' ||
       '            },' ||
       '            "BWA_MEM_OPTIONS": {' ||
       '                "value": "\" -T 0 \"",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "BWA_MEM_CONVEY_ADDITIONAL_OPTIONS": {' ||
       '                "value": "\"--bb_cny_timeout=5000000000 --bb_profile=1 -t 8\"",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates_optionMarkDuplicates": {' ||
       '                "value": "\" REMOVE_DUPLICATES=FALSE\"",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates_removeDuplicates": {' ||
       '                "value": "\" REMOVE_DUPLICATES=TRUE\"",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates_argumentList": {' ||
       '                "value": "${mergeAndRemoveDuplicates_optionMarkDuplicates}",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "LIB_ADD": {' ||
       '                "value": "addToOldLib"' ||
       '            },' ||
       '            "QUAL": {' ||
       '                "value": "phred"' ||
       '            },' ||
       '            "SNP_MINCOVERAGE": {' ||
       '                "value": "16"' ||
       '            },' ||
       '            "SNP_MAXCOVERAGE": {' ||
       '                "value": "300"' ||
       '            },' ||
       '            "CHROM_SIZES_FILE": {' ||
       '                "value": "${chromosomeSizesFile_hg19}",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "CLIP_INDEX": {' ||
       '                "value": "${DIR_EXECUTION}/analysisTools/qcPipelineTools/trimmomatic/adapters/TruSeq3-PE.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "ADAPTOR_TRIMMING_OPTIONS_0": {' ||
       '                "value": "\"PE -threads 12 -phred33\""' ||
       '            },' ||
       '            "ADAPTOR_TRIMMING_OPTIONS_1": {' ||
       '                "value": "\"ILLUMINACLIP:${CLIP_INDEX}:2:30:10:8:true SLIDINGWINDOW:4:15 MINLEN:36\""' ||
       '            },' ||
       '            "debugOptionsUseUndefinedVariableBreak": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "debugOptionsUseExitOnError": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "outputAllowAccessRightsModification": {' ||
       '                "value": "false"' ||
       '            },' ||
       '            "useBioBamBamSort": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "runFastQC": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "useCombinedAlignAndSampe": {' ||
       '                "value": "true",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "runSlimWorkflow": {' ||
       '                "value": "true",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "useAcceleratedHardware": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "BWA_VERSION": {' ||
       '                "value": "0.7.8",' ||
       '                "type": "string"' ||
       '            }' ||
       '        },' ||
       '        "resources": {' ||
       '            "cleanupScript": {' ||
       '                "memory": "0.1",' ||
       '                "value": "cleanupScript.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "1",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "fastqc": {' ||
       '                "value": "checkFastQC.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignment": {' ||
       '                "memory": "8",' ||
       '                "value": "bwaAlignSequence.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesort": {' ||
       '                "memory": "75",' ||
       '                "value": "bwaSampeSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesortSlim": {' ||
       '                "memory": "35",' ||
       '                "value": "bwaSampeSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPair": {' ||
       '                "memory": "20",' ||
       '                "value": "bwaMemSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "50",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPairSlim": {' ||
       '                "memory": "45",' ||
       '                "value": "bwaMemSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "180",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsIndex": {' ||
       '                "memory": "1",' ||
       '                "value": "samtoolsIndexBamfile.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "collectBamMetrics": {' ||
       '                "memory": "3",' ||
       '                "value": "picardCollectMetrics.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsFlagstat": {' ||
       '                "value": "samtoolsFlagstatBamfile.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "insertSizes": {' ||
       '                "value": "insertSizeDistribution.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "chromosomeDiff": {' ||
       '                "memory": "25",' ||
       '                "value": "differentiateChromosomes.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "genomeCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverage.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "readBinsCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverageReadBins.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlot": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlotSingle": {' ||
       '                "memory": "3",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates": {' ||
       '                "memory": "73",' ||
       '                "value": "mergeAndRemoveDuplicates.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimPicard": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimBioBambam": {' ||
       '                "memory": "15",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 3,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimSambamba": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "120",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            }' ||
       '        }' ||
       '    },' ||
       '    "RODDY_FILENAMES": {' ||
       '        "filenames": [' ||
       '            {' ||
       '                "class": "GenomeCoveragePlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${pid}_${sample[0]}_vs_${sample[1]}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
       '                "derivedFrom": "CoverageTextFile[2]"' ||
       '            },' ||
       '            {' ||
       '                "class": "GenomeCoveragePlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${pid}_${sample}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
       '                "selectiontag": "singlePlot",' ||
       '                "derivedFrom": "CoverageTextFile"' ||
       '            },' ||
       '            {' ||
       '                "class": "FastqcFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${fastx_qcOutputDirectory}/${dataSet}_${sample}_${run}_${lane}_${laneindex}_sequence_fastqc.zip",' ||
       '                "onMethod": "LaneFile.calcFastqc"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${fastx_qcOutputDirectory}/${dataSet}_${sample}_${run}_${lane}_${laneindex}_sequence_fastq_qcpass_status.txt",' ||
       '                "onMethod": "LaneFile.calcFastqc"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamIndexFile",' ||
       '                "pattern": "${sourcefile}.bai",' ||
       '                "derivedFrom": "BamFile"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${run}_${lane}_${cvalue,name=\"pairedBamSuffix\"}",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "FlagstatsFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
       '                "selectiontag": "extendedFlagstats",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamMetricsFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${sourcefileAtomic}.dupmark_metrics.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png_qcValues.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsizes.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "QCSummaryFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${sourcefileAtomicPrefix,delimiter=\"_\"}_${sourcefileProperty,type}_wroteQcSummary.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
       '                "selectiontag": "genomeCoverage",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
       '                "selectiontag": "readBinsCoverage",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
       '                "selectiontag": "fingerprints",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/qualitycontrol.json",' ||
       '                "selectiontag": "qcJson",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${pid}_${cvalue,name=\"defaultMergedBamSuffix\"}",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "FlagstatsFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
       '                "selectiontag": "extendedFlagstats",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamMetricsFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${sourcefileAtomic}.dupmark_metrics.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png_qcValues.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsizes.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "QCSummaryFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${sourcefileAtomicPrefix,delimiter=\"_\"}_${sourcefileProperty,type}_wroteQcSummary.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
       '                "selectiontag": "genomeCoverage",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
       '                "selectiontag": "readBinsCoverage",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/qualitycontrol.json",' ||
       '                "selectiontag": "qcJson",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamMetricsAlignmentSummaryFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${metricsOutputDirectory}/${cvalue,name=\"COLLECT_METRICS_PREFIX\"}.alignment_summary_metrics",' ||
       '                "onMethod": "BamFile.collectMetrics"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
       '                "selectiontag": "fingerprints",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorOutputDirectory}/${sample}_${pid}.readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.anno.txt.gz",' ||
       '                "selectiontag": "windowAnnotation",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.annotateCovWindows"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorOutputDirectory}/${sample}_${pid}_sex.txt",' ||
       '                "selectiontag": "sex",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.annotateCovWindows"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorOutputDirectory}/${sample}_${pid}_readCoverage_10kb_windows.filtered.txt.gz",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.mergeAndFilterCovWindows"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorOutputDirectory}/${sample}_${pid}_readCoverage_10kb_windows.filtered.corrected.txt.gz",' ||
       '                "selectiontag": "correctedWindows",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.correctGc"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorPlotOutputDirectory}/${sample}_${pid}_qc_gc_corrected.json",' ||
       '                "selectiontag": "correctedJson",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.correctGc"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorPlotOutputDirectory}/${sample}_${pid}_qc_gc_corrected.slim.tsv",' ||
       '                "selectiontag": "correctedTabSlim",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.correctGc"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorPlotOutputDirectory}/${sample}_${pid}_gc_corrected.png",' ||
       '                "selectiontag": "correctedGcPlot",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.correctGc"' ||
       '            }' ||
       '        ]' ||
       '    }' ||
       '}'
      )
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, selector_type, priority, external_workflow_config_fragment_id)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default values for PanCancer alignment WHOLE_GENOME', 'DEFAULT_VALUES', 22, (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default values for PanCancer alignment WHOLE_GENOME'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment WHOLE_GENOME'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment WHOLE_GENOME'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-202')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment WHOLE_GENOME'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-204')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment WHOLE_GENOME'), (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME' AND single_cell = false AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;


INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default values for PanCancer alignment 1.2.182 WHOLE_GENOME',
       '{' ||
       '    "RODDY": {' ||
       '        "cvalues": {' ||
       '            "workflowEnvironmentScript": {' ||
       '                "value": "workflowEnvironment_tbiLsf",' ||
       '                "type": "string"' ||
       '            }' ||
       '        },' ||
       '        "resources": {' ||
       '            "cleanupScript": {' ||
       '                "memory": "0.1",' ||
       '                "value": "cleanupScript.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "1",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "fastqc": {' ||
       '                "value": "checkFastQC.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignment": {' ||
       '                "memory": "8",' ||
       '                "value": "bwaAlignSequence.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesort": {' ||
       '                "memory": "75",' ||
       '                "value": "bwaSampeSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesortSlim": {' ||
       '                "memory": "35",' ||
       '                "value": "bwaSampeSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPair": {' ||
       '                "memory": "20",' ||
       '                "value": "bwaMemSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "50",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPairSlim": {' ||
       '                "memory": "20",' ||
       '                "value": "bwaMemSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "120",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsIndex": {' ||
       '                "memory": "1",' ||
       '                "value": "samtoolsIndexBamfile.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "collectBamMetrics": {' ||
       '                "memory": "3",' ||
       '                "value": "picardCollectMetrics.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "chromosomeDiff": {' ||
       '                "memory": "25",' ||
       '                "value": "differentiateChromosomes.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "genomeCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverage.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "readBinsCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverageReadBins.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlot": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlotSingle": {' ||
       '                "memory": "3",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates": {' ||
       '                "memory": "73",' ||
       '                "value": "mergeAndRemoveDuplicates.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimPicard": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimBioBambam": {' ||
       '                "memory": "15",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 3,' ||
       '                "basepath": "qcPipeline"' ||
       '            }' ||
       '        }' ||
       '    },' ||
       '    "RODDY_FILENAMES": {' ||
       '        ' ||
       '    }' ||
       '}'
      )
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, selector_type, priority, external_workflow_config_fragment_id)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default values for PanCancer alignment 1.2.182 WHOLE_GENOME', 'DEFAULT_VALUES', 22, (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default values for PanCancer alignment 1.2.182 WHOLE_GENOME'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.182 WHOLE_GENOME'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.182 WHOLE_GENOME'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.182')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.182 WHOLE_GENOME'), (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME' AND single_cell = false AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;


INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default values for PanCancer alignment 1.2.73-201, 1.2.73-3, 1.2.51-2, 1.2.51-1, 1.2.73-2, 1.2.73-1 ChIP Seq',
       '{' ||
       '    "RODDY": {' ||
       '        "cvalues": {' ||
       '            "workflowEnvironmentScript": {' ||
       '                "value": "workflowEnvironment_tbiLsf",' ||
       '                "type": "string"' ||
       '            }' ||
       '        },' ||
       '        "resources": {' ||
       '            "cleanupScript": {' ||
       '                "memory": "0.1",' ||
       '                "value": "cleanupScript.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "1",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "fastqc": {' ||
       '                "value": "checkFastQC.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignment": {' ||
       '                "memory": "8",' ||
       '                "value": "bwaAlignSequence.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesort": {' ||
       '                "memory": "75",' ||
       '                "value": "bwaSampeSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesortSlim": {' ||
       '                "memory": "35",' ||
       '                "value": "bwaSampeSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPair": {' ||
       '                "memory": "20",' ||
       '                "value": "bwaMemSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "50",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPairSlim": {' ||
       '                "memory": "50",' ||
       '                "value": "bwaMemSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "160",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsIndex": {' ||
       '                "memory": "1",' ||
       '                "value": "samtoolsIndexBamfile.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "collectBamMetrics": {' ||
       '                "memory": "3",' ||
       '                "value": "picardCollectMetrics.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsFlagstat": {' ||
       '                "value": "samtoolsFlagstatBamfile.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "insertSizes": {' ||
       '                "value": "insertSizeDistribution.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "chromosomeDiff": {' ||
       '                "memory": "25",' ||
       '                "value": "differentiateChromosomes.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "genomeCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverage.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "readBinsCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverageReadBins.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlot": {' ||
       '                "memory": "5",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlotSingle": {' ||
       '                "memory": "2",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates": {' ||
       '                "memory": "73",' ||
       '                "value": "mergeAndRemoveDuplicates.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimPicard": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimBioBambam": {' ||
       '                "memory": "15",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 3,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimSambamba": {' ||
       '                "memory": "80",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            }' ||
       '        }' ||
       '    },' ||
       '    "RODDY_FILENAMES": {' ||
       '        ' ||
       '    }' ||
       '}'
      )
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, selector_type, priority, external_workflow_config_fragment_id)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default values for PanCancer alignment 1.2.73-201, 1.2.73-3, 1.2.51-2, 1.2.51-1, 1.2.73-2, 1.2.73-1 ChIP Seq', 'DEFAULT_VALUES', 22, (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default values for PanCancer alignment 1.2.73-201, 1.2.73-3, 1.2.51-2, 1.2.51-1, 1.2.73-2, 1.2.73-1 ChIP Seq'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-201, 1.2.73-3, 1.2.51-2, 1.2.51-1, 1.2.73-2, 1.2.73-1 ChIP Seq'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-201, 1.2.73-3, 1.2.51-2, 1.2.51-1, 1.2.73-2, 1.2.73-1 ChIP Seq'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-201')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-201, 1.2.73-3, 1.2.51-2, 1.2.51-1, 1.2.73-2, 1.2.73-1 ChIP Seq'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-3')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-201, 1.2.73-3, 1.2.51-2, 1.2.51-1, 1.2.73-2, 1.2.73-1 ChIP Seq'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.51-2')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-201, 1.2.73-3, 1.2.51-2, 1.2.51-1, 1.2.73-2, 1.2.73-1 ChIP Seq'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.51-1')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-201, 1.2.73-3, 1.2.51-2, 1.2.51-1, 1.2.73-2, 1.2.73-1 ChIP Seq'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-2')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-201, 1.2.73-3, 1.2.51-2, 1.2.51-1, 1.2.73-2, 1.2.73-1 ChIP Seq'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-1')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.73-201, 1.2.73-3, 1.2.51-2, 1.2.51-1, 1.2.73-2, 1.2.73-1 ChIP Seq'), (SELECT id FROM seq_type WHERE name = 'ChIP Seq' AND single_cell = false AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;


INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default values for PanCancer alignment 1.2.51-1, 1.2.51-2 EXON',
       '{' ||
       '    "RODDY": {' ||
       '        "cvalues": {' ||
       '            "workflowEnvironmentScript": {' ||
       '                "value": "workflowEnvironment_tbiLsf",' ||
       '                "type": "string"' ||
       '            }' ||
       '        },' ||
       '        "resources": {' ||
       '            "cleanupScript": {' ||
       '                "memory": "0.1",' ||
       '                "value": "cleanupScript.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "1",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "fastqc": {' ||
       '                "value": "checkFastQC.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignment": {' ||
       '                "memory": "8",' ||
       '                "value": "bwaAlignSequence.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesort": {' ||
       '                "memory": "75",' ||
       '                "value": "bwaSampeSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesortSlim": {' ||
       '                "memory": "35",' ||
       '                "value": "bwaSampeSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPair": {' ||
       '                "memory": "20",' ||
       '                "value": "bwaMemSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "50",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPairSlim": {' ||
       '                "memory": "45",' ||
       '                "value": "bwaMemSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "160",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsIndex": {' ||
       '                "memory": "1",' ||
       '                "value": "samtoolsIndexBamfile.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "collectBamMetrics": {' ||
       '                "memory": "3",' ||
       '                "value": "picardCollectMetrics.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsFlagstat": {' ||
       '                "value": "samtoolsFlagstatBamfile.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "insertSizes": {' ||
       '                "value": "insertSizeDistribution.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "chromosomeDiff": {' ||
       '                "memory": "25",' ||
       '                "value": "differentiateChromosomes.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "genomeCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverage.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "readBinsCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverageReadBins.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlot": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlotSingle": {' ||
       '                "memory": "3",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates": {' ||
       '                "memory": "73",' ||
       '                "value": "mergeAndRemoveDuplicates.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimPicard": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimBioBambam": {' ||
       '                "memory": "15",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 3,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimSambamba": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "targetExtractCoverageSlim": {' ||
       '                "memory": "15",' ||
       '                "value": "targetExtractCoverageSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "8",' ||
       '                "cores": 2,' ||
       '                "basepath": "exomePipeline"' ||
       '            }' ||
       '        }' ||
       '    },' ||
       '    "RODDY_FILENAMES": {' ||
       '        ' ||
       '    }' ||
       '}'
      )
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, selector_type, priority, external_workflow_config_fragment_id)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default values for PanCancer alignment 1.2.51-1, 1.2.51-2 EXON', 'DEFAULT_VALUES', 22, (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default values for PanCancer alignment 1.2.51-1, 1.2.51-2 EXON'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.51-1, 1.2.51-2 EXON'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.51-1, 1.2.51-2 EXON'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.51-1')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.51-1, 1.2.51-2 EXON'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.51-2')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.51-1, 1.2.51-2 EXON'), (SELECT id FROM seq_type WHERE name = 'EXON' AND single_cell = false AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;


INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default values for PanCancer alignment ChIP Seq',
       '{' ||
       '    "RODDY": {' ||
       '        "cvalues": {' ||
       '            "workflowEnvironmentScript": {' ||
       '                "value": "workflowEnvironment_tbiLsf",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "sampleDirectory": {' ||
       '                "value": "${inputBaseDirectory}/${pid}/${sample}/${SEQUENCER_PROTOCOL}",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "sequenceDirectory": {' ||
       '                "value": "${sampleDirectory}/${run}/sequence",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "outputAnalysisBaseDirectory": {' ||
       '                "value": "${outputBaseDirectory}",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "outputExecutionDirectory": {' ||
       '                "value": "${outputAnalysisBaseDirectory}/exec_${executionTimeString}"' ||
       '            },' ||
       '            "mergedBamSuffix_markDuplicates": {' ||
       '                "value": "merged.bam.dupmarked.bam",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergedBamSuffix_markDuplicatesShort": {' ||
       '                "value": "merged.mdup.bam",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergedBamSuffix_removeDuplicates": {' ||
       '                "value": "merged.bam.rmdup.bam",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergedBamSuffixList": {' ||
       '                "value": "${mergedBamSuffix_markDuplicates},${mergedBamSuffix_markDuplicatesShort},${mergedBamSuffix_removeDuplicates}",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "defaultMergedBamSuffix": {' ||
       '                "value": "${mergedBamSuffix_markDuplicatesShort}",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "pairedBamSuffix": {' ||
       '                "value": "paired.bam.sorted.bam",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "alignmentOutputDirectory": {' ||
       '                "value": "alignment"' ||
       '            },' ||
       '            "fastx_qcOutputDirectory": {' ||
       '                "value": "fastx_qc"' ||
       '            },' ||
       '            "coverageOutputDirectory": {' ||
       '                "value": "coverage"' ||
       '            },' ||
       '            "flagstatsOutputDirectory": {' ||
       '                "value": "flagstats"' ||
       '            },' ||
       '            "structuralVariationOutputDirectory": {' ||
       '                "value": "structural_variation"' ||
       '            },' ||
       '            "insertSizesOutputDirectory": {' ||
       '                "value": "insertsize_distribution"' ||
       '            },' ||
       '            "metricsOutputDirectory": {' ||
       '                "value": "metrics"' ||
       '            },' ||
       '            "mpileupOutputDirectory": {' ||
       '                "value": "mpileup"' ||
       '            },' ||
       '            "mpileupPlatypusOutputDirectory": {' ||
       '                "value": "platypus_indel"' ||
       '            },' ||
       '            "assembliesBaseDirectory": {' ||
       '                "value": "${sharedFilesBaseDirectory}/assemblies",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "assembliesHG191000GenomesDirectory": {' ||
       '                "value": "${assembliesBaseDirectory}/hg19_GRCh37_1000genomes",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "bwaIndexBaseDirectory_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/indexes/bwa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "bwaIndexBaseDirectory_methylCtools_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/indexes/methylCtools",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "referenceGenomeBaseDirectory_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/sequence",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesBaseDirectory_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/stats",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegionsBaseDirectory_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/targetRegions",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabasesDirectory": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/databases",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabaseUCSCDirectory": {' ||
       '                "value": "${hg19DatabasesDirectory}/UCSC",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabaseDBSNPDirectory": {' ||
       '                "value": "${hg19DatabasesDirectory}/dbSNP",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19Database1000GenomesDirectory": {' ||
       '                "value": "${hg19DatabasesDirectory}/1000genomes",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabaseIMPUTEDirectory": {' ||
       '                "value": "${hg19Database1000GenomesDirectory}/IMPUTE",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabaseENCODEDirectory": {' ||
       '                "value": "${hg19DatabasesDirectory}/ENCODE",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "bwaIndexBaseDirectory_mm10": {' ||
       '                "value": "${assembliesBaseDirectory}/mm10/indexes/bwa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "bwaIndexBaseDirectory_methylCtools_mm10": {' ||
       '                "value": "${assembliesBaseDirectory}/mm10/indexes/methylCtools",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesBaseDirectory_mm10": {' ||
       '                "value": "${assembliesBaseDirectory}/mm10/stats",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegionsBaseDirectory_mm10": {' ||
       '                "value": "${assembliesBaseDirectory}/mm10/targetRegions",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "meth_calls_converter_moabs": {' ||
       '                "value": "",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "indexPrefix_bwa05_1KGRef": {' ||
       '                "value": "${bwaIndexBaseDirectory_human}/bwa05_1KGRef/hs37d5.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa05_hg19_chr": {' ||
       '                "value": "${bwaIndexBaseDirectory_human}/bwa05_hg19_chr/hg19bwaidx",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_1KGRef": {' ||
       '                "value": "${bwaIndexBaseDirectory_human}/bwa06_1KGRef/hs37d5.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_hg19_chr": {' ||
       '                "value": "${bwaIndexBaseDirectory_human}/bwa06_hg19_chr/hg19_1-22_X_Y_M.fasta",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_mm10_GRC": {' ||
       '                "value": "${bwaIndexBaseDirectory_mm10}/bwa06/bwa06_GRCm38mm10/GRCm38mm10.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_mm10": {' ||
       '                "value": "${bwaIndexBaseDirectory_mm10}/bwa06/bwa06_mm10_UCSC/mm10_1-19_X_Y_M.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_methylCtools_mm10_GRC": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_GRCm38mm10/GRCm38mm10_PhiX_Lambda.conv.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_methylCtools_mm10_UCSC": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_mm10_UCSC/mm10_PhiX_Lambda.conv.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_methylCtools_1KGRef": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_human}/methylCtools_1KGRef/hs37d5_PhiX_Lambda.conv.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "ch_pos_index_methylCtools_1KGRef": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_human}/methylCtools_1KGRef/hs37d5_PhiX_Lambda.CG_CH.pos.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "ch_pos_index_methylCtools_mm10GRC": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_GRCm38mm10/GRCm38mm10_PhiX_Lambda.pos.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "ch_pos_index_methylCtools_mm10_UCSC": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_mm10_UCSC/mm10_PhiX_Lambda.pos.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "referenceGenome_1KGRef": {' ||
       '                "value": "${referenceGenomeBaseDirectory_human}/1KGRef/hs37d5.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "referenceGenome_hg19_chr": {' ||
       '                "value": "${referenceGenomeBaseDirectory_human}/hg19_chr/hg19_1-22_X_Y_M.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_hg19": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_human}/hg19_1-22_X_Y_M.fa.chrLenOnlyACGT.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_hs37": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_human}/hs37d5.fa.chrLenOnlyACGT_realChromosomes.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_mm10_GRC": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_mm10}/GRCm38mm10.fa.chrLenOnlyACGT_realChromosomes.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_mm10": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_mm10}/mm10_1-19_X_Y_M.fa.chrLenOnlyACGT_realChromosomes.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_hs37_bisulfite": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_human}/hs37d5_PhiX_Lambda.fa.chrLenOnlyACGT.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_mm10_GRC_bisulfite": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_mm10}/GRCm38mm10_PhiX_Lambda.fa.chrLenOnlyACGT.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_mm10_UCSC_bisulfite": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_mm10}/mm10_PhiX_Lambda.fa.chrLenOnlyACGT.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeLengthFile_hg19": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_human}/hg19_chrTotalLength.tsv",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent4withoutUTRs_chr": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent4withoutUTRs_chr.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent4withoutUTRs_plain": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent4withoutUTRs_plain.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent4withUTRs_plain": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent4withUTRs_plain.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent5withoutUTRs_chr": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withoutUTRs_chr.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent5withoutUTRs_plain": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withoutUTRs_plain.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent5withUTRs_chr": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withUTRs_chr.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent5withUTRs_plain": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withUTRs_plain.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "dbSNP_FILE": {' ||
       '                "value": "${hg19DatabasesDirectory}/dbSNP/dbSNP_135/00-All.SNV.vcf.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "SNP_REFERENCE": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/sequence/hg19_chr/hg19_1-22_X_Y_M.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "SNP_REFERENCE_ANNOTATIONS": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/Affymetrics/Affy5/chr/#CHROM#_AFFY.vcf",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "alignmentThreads": {' ||
       '                "value": "12",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "outputUMask": {' ||
       '                "value": "007",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "outputFileGroup": {' ||
       '                "value": "false"' ||
       '            },' ||
       '            "outputAccessRights": {' ||
       '                "value": "u+rw,g+rw,o-rwx"' ||
       '            },' ||
       '            "outputAccessRightsForDirectories": {' ||
       '                "value": "u+rwx,g+rwx,o-rwx"' ||
       '            },' ||
       '            "possibleControlSampleNamePrefixes": {' ||
       '                "value": "( blood BLOOD normal control CONTROL buffy_coat GERMLINE )",' ||
       '                "type": "bashArray"' ||
       '            },' ||
       '            "possibleTumorSampleNamePrefixes": {' ||
       '                "value": "( tumor TUMOR metastasis xenograft disease DISEASE relapse RELAPSE autopsy AUTOPSY metastasis METASTASIS )",' ||
       '                "type": "bashArray"' ||
       '            },' ||
       '            "useCentralAnalysisArchive": {' ||
       '                "value": "true"' ||
       '            },' ||
       '            "enableJobProfiling": {' ||
       '                "value": "false"' ||
       '            },' ||
       '            "JOB_PROFILER_BINARY": {' ||
       '                "value": "strace.sh"' ||
       '            },' ||
       '            "INDEX_PREFIX": {' ||
       '                "value": "${indexPrefix_bwa05_hg19_chr}",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "BWA_ALIGNMENT_OPTIONS": {' ||
       '                "value": "\"-q 20\""' ||
       '            },' ||
       '            "BWA_SAMPESORT_OPTIONS": {' ||
       '                "value": "\"-a 1000\""' ||
       '            },' ||
       '            "SAMPESORT_MEMSIZE": {' ||
       '                "value": 2000000000,' ||
       '                "type": "integer"' ||
       '            },' ||
       '            "BWA_MEM_OPTIONS": {' ||
       '                "value": "\" -T 0 \"",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "BWA_MEM_CONVEY_ADDITIONAL_OPTIONS": {' ||
       '                "value": "\"--bb_cny_timeout=5000000000 --bb_profile=1 -t 8\"",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates_optionMarkDuplicates": {' ||
       '                "value": "\" REMOVE_DUPLICATES=FALSE\"",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates_removeDuplicates": {' ||
       '                "value": "\" REMOVE_DUPLICATES=TRUE\"",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates_argumentList": {' ||
       '                "value": "${mergeAndRemoveDuplicates_optionMarkDuplicates}",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "LIB_ADD": {' ||
       '                "value": "addToOldLib"' ||
       '            },' ||
       '            "QUAL": {' ||
       '                "value": "phred"' ||
       '            },' ||
       '            "SNP_MINCOVERAGE": {' ||
       '                "value": "16"' ||
       '            },' ||
       '            "SNP_MAXCOVERAGE": {' ||
       '                "value": "300"' ||
       '            },' ||
       '            "CHROM_SIZES_FILE": {' ||
       '                "value": "${chromosomeSizesFile_hg19}",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "CLIP_INDEX": {' ||
       '                "value": "${DIR_EXECUTION}/analysisTools/qcPipelineTools/trimmomatic/adapters/TruSeq3-PE.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "ADAPTOR_TRIMMING_OPTIONS_0": {' ||
       '                "value": "\"PE -threads 12 -phred33\""' ||
       '            },' ||
       '            "ADAPTOR_TRIMMING_OPTIONS_1": {' ||
       '                "value": "\"ILLUMINACLIP:${CLIP_INDEX}:2:30:10:8:true SLIDINGWINDOW:4:15 MINLEN:36\""' ||
       '            },' ||
       '            "debugOptionsUseUndefinedVariableBreak": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "outputAllowAccessRightsModification": {' ||
       '                "value": "false"' ||
       '            },' ||
       '            "useBioBamBamSort": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "runFastQC": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "useCombinedAlignAndSampe": {' ||
       '                "value": "true",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "runSlimWorkflow": {' ||
       '                "value": "true",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "useAcceleratedHardware": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "BWA_VERSION": {' ||
       '                "value": "0.7.8",' ||
       '                "type": "string"' ||
       '            }' ||
       '        },' ||
       '        "resources": {' ||
       '            "cleanupScript": {' ||
       '                "memory": "0.1",' ||
       '                "value": "cleanupScript.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "1",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "fastqc": {' ||
       '                "value": "checkFastQC.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignment": {' ||
       '                "memory": "8",' ||
       '                "value": "bwaAlignSequence.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesort": {' ||
       '                "memory": "75",' ||
       '                "value": "bwaSampeSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesortSlim": {' ||
       '                "memory": "35",' ||
       '                "value": "bwaSampeSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPair": {' ||
       '                "memory": "20",' ||
       '                "value": "bwaMemSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "50",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPairSlim": {' ||
       '                "memory": "50",' ||
       '                "value": "bwaMemSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "160",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsIndex": {' ||
       '                "memory": "1",' ||
       '                "value": "samtoolsIndexBamfile.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "collectBamMetrics": {' ||
       '                "memory": "3",' ||
       '                "value": "picardCollectMetrics.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsFlagstat": {' ||
       '                "value": "samtoolsFlagstatBamfile.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "insertSizes": {' ||
       '                "value": "insertSizeDistribution.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "chromosomeDiff": {' ||
       '                "memory": "25",' ||
       '                "value": "differentiateChromosomes.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "genomeCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverage.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "readBinsCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverageReadBins.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlot": {' ||
       '                "memory": "5",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlotSingle": {' ||
       '                "memory": "2",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates": {' ||
       '                "memory": "73",' ||
       '                "value": "mergeAndRemoveDuplicates.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimPicard": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimBioBambam": {' ||
       '                "memory": "15",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 3,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimSambamba": {' ||
       '                "memory": "80",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            }' ||
       '        }' ||
       '    },' ||
       '    "RODDY_FILENAMES": {' ||
       '        "filenames": [' ||
       '            {' ||
       '                "class": "GenomeCoveragePlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${pid}_${sample[0]}_vs_${sample[1]}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
       '                "derivedFrom": "CoverageTextFile[2]"' ||
       '            },' ||
       '            {' ||
       '                "class": "GenomeCoveragePlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${pid}_${sample}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
       '                "selectiontag": "singlePlot",' ||
       '                "derivedFrom": "CoverageTextFile"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamIndexFile",' ||
       '                "pattern": "${sourcefile}.bai",' ||
       '                "derivedFrom": "BamFile"' ||
       '            },' ||
       '            {' ||
       '                "class": "FastqcFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${fastx_qcOutputDirectory}/${dataSet}_${sample}_${run}_${laneindex}_sequence_fastqc.zip",' ||
       '                "onMethod": "LaneFile.calcFastqc"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${run}_${lane}_${cvalue,name=\"pairedBamSuffix\"}",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "FlagstatsFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
       '                "selectiontag": "extendedFlagstats",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamMetricsFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${sourcefileAtomic}.dupmark_metrics.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png_qcValues.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsizes.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "QCSummaryFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${sourcefileAtomicPrefix,delimiter=\"_\"}_${sourcefileProperty,type}_wroteQcSummary.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
       '                "selectiontag": "genomeCoverage",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
       '                "selectiontag": "readBinsCoverage",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
       '                "selectiontag": "fingerprints",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/qualitycontrol.json",' ||
       '                "selectiontag": "qcJson",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${pid}_${cvalue,name=\"defaultMergedBamSuffix\"}",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "FlagstatsFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
       '                "selectiontag": "extendedFlagstats",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamMetricsFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${sourcefileAtomic}.dupmark_metrics.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png_qcValues.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsizes.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "QCSummaryFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${sourcefileAtomicPrefix,delimiter=\"_\"}_${sourcefileProperty,type}_wroteQcSummary.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
       '                "selectiontag": "genomeCoverage",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
       '                "selectiontag": "readBinsCoverage",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/qualitycontrol.json",' ||
       '                "selectiontag": "qcJson",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamMetricsAlignmentSummaryFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${metricsOutputDirectory}/${cvalue,name=\"COLLECT_METRICS_PREFIX\"}.alignment_summary_metrics",' ||
       '                "onMethod": "BamFile.collectMetrics"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
       '                "selectiontag": "fingerprints",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            }' ||
       '        ]' ||
       '    }' ||
       '}'
      )
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, selector_type, priority, external_workflow_config_fragment_id)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default values for PanCancer alignment ChIP Seq', 'DEFAULT_VALUES', 22, (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default values for PanCancer alignment ChIP Seq'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment ChIP Seq'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment ChIP Seq'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-202')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment ChIP Seq'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-204')
ON CONFLICT DO NOTHING;


INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment ChIP Seq'), (SELECT id FROM seq_type WHERE name = 'ChIP Seq' AND single_cell = false AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;


INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default values for PanCancer alignment 1.2.182 EXON',
       '{' ||
       '    "RODDY": {' ||
       '        "cvalues": {' ||
       '            "workflowEnvironmentScript": {' ||
       '                "value": "workflowEnvironment_tbiLsf",' ||
       '                "type": "string"' ||
       '            }' ||
       '        },' ||
       '        "resources": {' ||
       '            "cleanupScript": {' ||
       '                "memory": "0.1",' ||
       '                "value": "cleanupScript.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "1",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "fastqc": {' ||
       '                "value": "checkFastQC.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignment": {' ||
       '                "memory": "8",' ||
       '                "value": "bwaAlignSequence.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesort": {' ||
       '                "memory": "75",' ||
       '                "value": "bwaSampeSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesortSlim": {' ||
       '                "memory": "35",' ||
       '                "value": "bwaSampeSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPair": {' ||
       '                "memory": "20",' ||
       '                "value": "bwaMemSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "50",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPairSlim": {' ||
       '                "memory": "20",' ||
       '                "value": "bwaMemSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "120",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsIndex": {' ||
       '                "memory": "1",' ||
       '                "value": "samtoolsIndexBamfile.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "collectBamMetrics": {' ||
       '                "memory": "3",' ||
       '                "value": "picardCollectMetrics.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "chromosomeDiff": {' ||
       '                "memory": "25",' ||
       '                "value": "differentiateChromosomes.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "genomeCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverage.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "readBinsCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverageReadBins.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlot": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlotSingle": {' ||
       '                "memory": "2",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates": {' ||
       '                "memory": "73",' ||
       '                "value": "mergeAndRemoveDuplicates.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimPicard": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimBioBambam": {' ||
       '                "memory": "15",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 3,' ||
       '                "basepath": "qcPipeline"' ||
       '            }' ||
       '        }' ||
       '    },' ||
       '    "RODDY_FILENAMES": {' ||
       '        ' ||
       '    }' ||
       '}'
      )
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, selector_type, priority, external_workflow_config_fragment_id)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default values for PanCancer alignment 1.2.182 EXON', 'DEFAULT_VALUES', 22, (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default values for PanCancer alignment 1.2.182 EXON'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.182 EXON'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.182 EXON'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.182')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment 1.2.182 EXON'), (SELECT id FROM seq_type WHERE name = 'EXON' AND single_cell = false AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;


INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default values for PanCancer alignment EXON',
       '{' ||
       '    "RODDY": {' ||
       '        "cvalues": {' ||
       '            "workflowEnvironmentScript": {' ||
       '                "value": "workflowEnvironment_tbiLsf",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "sampleDirectory": {' ||
       '                "value": "${inputBaseDirectory}/${pid}/${sample}/${SEQUENCER_PROTOCOL}",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "sequenceDirectory": {' ||
       '                "value": "${sampleDirectory}/${run}/sequence",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "outputAnalysisBaseDirectory": {' ||
       '                "value": "${outputBaseDirectory}",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "outputExecutionDirectory": {' ||
       '                "value": "${outputAnalysisBaseDirectory}/exec_${executionTimeString}"' ||
       '            },' ||
       '            "mergedBamSuffix_markDuplicates": {' ||
       '                "value": "merged.bam.dupmarked.bam",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergedBamSuffix_markDuplicatesShort": {' ||
       '                "value": "merged.mdup.bam",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergedBamSuffix_removeDuplicates": {' ||
       '                "value": "merged.bam.rmdup.bam",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergedBamSuffixList": {' ||
       '                "value": "${mergedBamSuffix_markDuplicates},${mergedBamSuffix_markDuplicatesShort},${mergedBamSuffix_removeDuplicates}",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "defaultMergedBamSuffix": {' ||
       '                "value": "${mergedBamSuffix_markDuplicatesShort}",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "pairedBamSuffix": {' ||
       '                "value": "paired.bam.sorted.bam",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "alignmentOutputDirectory": {' ||
       '                "value": "alignment"' ||
       '            },' ||
       '            "fastx_qcOutputDirectory": {' ||
       '                "value": "fastx_qc"' ||
       '            },' ||
       '            "coverageOutputDirectory": {' ||
       '                "value": "coverage"' ||
       '            },' ||
       '            "flagstatsOutputDirectory": {' ||
       '                "value": "flagstats"' ||
       '            },' ||
       '            "structuralVariationOutputDirectory": {' ||
       '                "value": "structural_variation"' ||
       '            },' ||
       '            "insertSizesOutputDirectory": {' ||
       '                "value": "insertsize_distribution"' ||
       '            },' ||
       '            "metricsOutputDirectory": {' ||
       '                "value": "metrics"' ||
       '            },' ||
       '            "mpileupOutputDirectory": {' ||
       '                "value": "mpileup"' ||
       '            },' ||
       '            "mpileupPlatypusOutputDirectory": {' ||
       '                "value": "platypus_indel"' ||
       '            },' ||
       '            "assembliesBaseDirectory": {' ||
       '                "value": "${sharedFilesBaseDirectory}/assemblies",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "assembliesHG191000GenomesDirectory": {' ||
       '                "value": "${assembliesBaseDirectory}/hg19_GRCh37_1000genomes",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "bwaIndexBaseDirectory_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/indexes/bwa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "bwaIndexBaseDirectory_methylCtools_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/indexes/methylCtools",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "referenceGenomeBaseDirectory_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/sequence",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesBaseDirectory_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/stats",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegionsBaseDirectory_human": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/targetRegions",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabasesDirectory": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/databases",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabaseUCSCDirectory": {' ||
       '                "value": "${hg19DatabasesDirectory}/UCSC",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabaseDBSNPDirectory": {' ||
       '                "value": "${hg19DatabasesDirectory}/dbSNP",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19Database1000GenomesDirectory": {' ||
       '                "value": "${hg19DatabasesDirectory}/1000genomes",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabaseIMPUTEDirectory": {' ||
       '                "value": "${hg19Database1000GenomesDirectory}/IMPUTE",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "hg19DatabaseENCODEDirectory": {' ||
       '                "value": "${hg19DatabasesDirectory}/ENCODE",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "bwaIndexBaseDirectory_mm10": {' ||
       '                "value": "${assembliesBaseDirectory}/mm10/indexes/bwa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "bwaIndexBaseDirectory_methylCtools_mm10": {' ||
       '                "value": "${assembliesBaseDirectory}/mm10/indexes/methylCtools",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesBaseDirectory_mm10": {' ||
       '                "value": "${assembliesBaseDirectory}/mm10/stats",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegionsBaseDirectory_mm10": {' ||
       '                "value": "${assembliesBaseDirectory}/mm10/targetRegions",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "meth_calls_converter_moabs": {' ||
       '                "value": "",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "indexPrefix_bwa05_1KGRef": {' ||
       '                "value": "${bwaIndexBaseDirectory_human}/bwa05_1KGRef/hs37d5.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa05_hg19_chr": {' ||
       '                "value": "${bwaIndexBaseDirectory_human}/bwa05_hg19_chr/hg19bwaidx",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_1KGRef": {' ||
       '                "value": "${bwaIndexBaseDirectory_human}/bwa06_1KGRef/hs37d5.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_hg19_chr": {' ||
       '                "value": "${bwaIndexBaseDirectory_human}/bwa06_hg19_chr/hg19_1-22_X_Y_M.fasta",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_mm10_GRC": {' ||
       '                "value": "${bwaIndexBaseDirectory_mm10}/bwa06/bwa06_GRCm38mm10/GRCm38mm10.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_mm10": {' ||
       '                "value": "${bwaIndexBaseDirectory_mm10}/bwa06/bwa06_mm10_UCSC/mm10_1-19_X_Y_M.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_methylCtools_mm10_GRC": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_GRCm38mm10/GRCm38mm10_PhiX_Lambda.conv.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_methylCtools_mm10_UCSC": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_mm10_UCSC/mm10_PhiX_Lambda.conv.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "indexPrefix_bwa06_methylCtools_1KGRef": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_human}/methylCtools_1KGRef/hs37d5_PhiX_Lambda.conv.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "ch_pos_index_methylCtools_1KGRef": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_human}/methylCtools_1KGRef/hs37d5_PhiX_Lambda.CG_CH.pos.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "ch_pos_index_methylCtools_mm10GRC": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_GRCm38mm10/GRCm38mm10_PhiX_Lambda.pos.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "ch_pos_index_methylCtools_mm10_UCSC": {' ||
       '                "value": "${bwaIndexBaseDirectory_methylCtools_mm10}/methylCtools_mm10_UCSC/mm10_PhiX_Lambda.pos.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "referenceGenome_1KGRef": {' ||
       '                "value": "${referenceGenomeBaseDirectory_human}/1KGRef/hs37d5.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "referenceGenome_hg19_chr": {' ||
       '                "value": "${referenceGenomeBaseDirectory_human}/hg19_chr/hg19_1-22_X_Y_M.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_hg19": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_human}/hg19_1-22_X_Y_M.fa.chrLenOnlyACGT.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_hs37": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_human}/hs37d5.fa.chrLenOnlyACGT_realChromosomes.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_mm10_GRC": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_mm10}/GRCm38mm10.fa.chrLenOnlyACGT_realChromosomes.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_mm10": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_mm10}/mm10_1-19_X_Y_M.fa.chrLenOnlyACGT_realChromosomes.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_hs37_bisulfite": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_human}/hs37d5_PhiX_Lambda.fa.chrLenOnlyACGT.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_mm10_GRC_bisulfite": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_mm10}/GRCm38mm10_PhiX_Lambda.fa.chrLenOnlyACGT.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeSizesFile_mm10_UCSC_bisulfite": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_mm10}/mm10_PhiX_Lambda.fa.chrLenOnlyACGT.tab",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "chromosomeLengthFile_hg19": {' ||
       '                "value": "${chromosomeSizesBaseDirectory_human}/hg19_chrTotalLength.tsv",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent4withoutUTRs_chr": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent4withoutUTRs_chr.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent4withoutUTRs_plain": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent4withoutUTRs_plain.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent4withUTRs_plain": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent4withUTRs_plain.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent5withoutUTRs_chr": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withoutUTRs_chr.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent5withoutUTRs_plain": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withoutUTRs_plain.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent5withUTRs_chr": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withUTRs_chr.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "targetRegions_Agilent5withUTRs_plain": {' ||
       '                "value": "${targetRegionsBaseDirectory_human}/Agilent5withUTRs_plain.bed.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "dbSNP_FILE": {' ||
       '                "value": "${hg19DatabasesDirectory}/dbSNP/dbSNP_135/00-All.SNV.vcf.gz",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "SNP_REFERENCE": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/sequence/hg19_chr/hg19_1-22_X_Y_M.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "SNP_REFERENCE_ANNOTATIONS": {' ||
       '                "value": "${assembliesHG191000GenomesDirectory}/Affymetrics/Affy5/chr/#CHROM#_AFFY.vcf",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "alignmentThreads": {' ||
       '                "value": "12",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "outputUMask": {' ||
       '                "value": "007",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "outputFileGroup": {' ||
       '                "value": "false"' ||
       '            },' ||
       '            "outputAccessRights": {' ||
       '                "value": "u+rw,g+rw,o-rwx"' ||
       '            },' ||
       '            "outputAccessRightsForDirectories": {' ||
       '                "value": "u+rwx,g+rwx,o-rwx"' ||
       '            },' ||
       '            "possibleControlSampleNamePrefixes": {' ||
       '                "value": "( blood BLOOD normal control CONTROL buffy_coat GERMLINE )",' ||
       '                "type": "bashArray"' ||
       '            },' ||
       '            "possibleTumorSampleNamePrefixes": {' ||
       '                "value": "( tumor TUMOR metastasis xenograft disease DISEASE relapse RELAPSE autopsy AUTOPSY metastasis METASTASIS )",' ||
       '                "type": "bashArray"' ||
       '            },' ||
       '            "useCentralAnalysisArchive": {' ||
       '                "value": "true"' ||
       '            },' ||
       '            "enableJobProfiling": {' ||
       '                "value": "false"' ||
       '            },' ||
       '            "JOB_PROFILER_BINARY": {' ||
       '                "value": "strace.sh"' ||
       '            },' ||
       '            "INDEX_PREFIX": {' ||
       '                "value": "${indexPrefix_bwa05_hg19_chr}",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "BWA_ALIGNMENT_OPTIONS": {' ||
       '                "value": "\"-q 20\""' ||
       '            },' ||
       '            "BWA_SAMPESORT_OPTIONS": {' ||
       '                "value": "\"-a 1000\""' ||
       '            },' ||
       '            "SAMPESORT_MEMSIZE": {' ||
       '                "value": 2000000000,' ||
       '                "type": "integer"' ||
       '            },' ||
       '            "BWA_MEM_OPTIONS": {' ||
       '                "value": "\" -T 0 \"",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "BWA_MEM_CONVEY_ADDITIONAL_OPTIONS": {' ||
       '                "value": "\"--bb_cny_timeout=5000000000 --bb_profile=1 -t 8\"",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates_optionMarkDuplicates": {' ||
       '                "value": "\" REMOVE_DUPLICATES=FALSE\"",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates_removeDuplicates": {' ||
       '                "value": "\" REMOVE_DUPLICATES=TRUE\"",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates_argumentList": {' ||
       '                "value": "${mergeAndRemoveDuplicates_optionMarkDuplicates}",' ||
       '                "type": "string"' ||
       '            },' ||
       '            "LIB_ADD": {' ||
       '                "value": "addToOldLib"' ||
       '            },' ||
       '            "QUAL": {' ||
       '                "value": "phred"' ||
       '            },' ||
       '            "SNP_MINCOVERAGE": {' ||
       '                "value": "16"' ||
       '            },' ||
       '            "SNP_MAXCOVERAGE": {' ||
       '                "value": "300"' ||
       '            },' ||
       '            "CHROM_SIZES_FILE": {' ||
       '                "value": "${chromosomeSizesFile_hg19}",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "CLIP_INDEX": {' ||
       '                "value": "${DIR_EXECUTION}/analysisTools/qcPipelineTools/trimmomatic/adapters/TruSeq3-PE.fa",' ||
       '                "type": "path"' ||
       '            },' ||
       '            "ADAPTOR_TRIMMING_OPTIONS_0": {' ||
       '                "value": "\"PE -threads 12 -phred33\""' ||
       '            },' ||
       '            "ADAPTOR_TRIMMING_OPTIONS_1": {' ||
       '                "value": "\"ILLUMINACLIP:${CLIP_INDEX}:2:30:10:8:true SLIDINGWINDOW:4:15 MINLEN:36\""' ||
       '            },' ||
       '            "debugOptionsUseUndefinedVariableBreak": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "debugOptionsUseExitOnError": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "outputAllowAccessRightsModification": {' ||
       '                "value": "false"' ||
       '            },' ||
       '            "useBioBamBamSort": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "runFastQC": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "useCombinedAlignAndSampe": {' ||
       '                "value": "true",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "runSlimWorkflow": {' ||
       '                "value": "true",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "useAcceleratedHardware": {' ||
       '                "value": "false",' ||
       '                "type": "boolean"' ||
       '            },' ||
       '            "BWA_VERSION": {' ||
       '                "value": "0.7.8",' ||
       '                "type": "string"' ||
       '            }' ||
       '        },' ||
       '        "resources": {' ||
       '            "cleanupScript": {' ||
       '                "memory": "0.1",' ||
       '                "value": "cleanupScript.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "1",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "fastqc": {' ||
       '                "value": "checkFastQC.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignment": {' ||
       '                "memory": "8",' ||
       '                "value": "bwaAlignSequence.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesort": {' ||
       '                "memory": "75",' ||
       '                "value": "bwaSampeSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "sampesortSlim": {' ||
       '                "memory": "35",' ||
       '                "value": "bwaSampeSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPair": {' ||
       '                "memory": "20",' ||
       '                "value": "bwaMemSort.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "50",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "alignAndPairSlim": {' ||
       '                "memory": "45",' ||
       '                "value": "bwaMemSortSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "180",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsIndex": {' ||
       '                "memory": "1",' ||
       '                "value": "samtoolsIndexBamfile.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "collectBamMetrics": {' ||
       '                "memory": "3",' ||
       '                "value": "picardCollectMetrics.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "samtoolsFlagstat": {' ||
       '                "value": "samtoolsFlagstatBamfile.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "insertSizes": {' ||
       '                "value": "insertSizeDistribution.sh",' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "chromosomeDiff": {' ||
       '                "memory": "25",' ||
       '                "value": "differentiateChromosomes.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "5",' ||
       '                "cores": 1,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "genomeCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverage.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "readBinsCoverage": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoverageReadBins.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlot": {' ||
       '                "memory": "0.05",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "6",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "coveragePlotSingle": {' ||
       '                "memory": "3",' ||
       '                "value": "genomeCoveragePlots.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "10",' ||
       '                "cores": 4,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicates": {' ||
       '                "memory": "73",' ||
       '                "value": "mergeAndRemoveDuplicates.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "80",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimPicard": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 8,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimBioBambam": {' ||
       '                "memory": "15",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "240",' ||
       '                "cores": 3,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "mergeAndRemoveDuplicatesSlimSambamba": {' ||
       '                "memory": "100",' ||
       '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "120",' ||
       '                "cores": 6,' ||
       '                "basepath": "qcPipeline"' ||
       '            },' ||
       '            "targetExtractCoverageSlim": {' ||
       '                "memory": "15",' ||
       '                "value": "targetExtractCoverageSlim.sh",' ||
       '                "nodes": 1,' ||
       '                "walltime": "12",' ||
       '                "cores": 2,' ||
       '                "basepath": "exomePipeline"' ||
       '            }' ||
       '        }' ||
       '    },' ||
       '    "RODDY_FILENAMES": {' ||
       '        "filenames": [' ||
       '            {' ||
       '                "class": "GenomeCoveragePlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${pid}_${sample[0]}_vs_${sample[1]}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
       '                "derivedFrom": "CoverageTextFile[2]"' ||
       '            },' ||
       '            {' ||
       '                "class": "GenomeCoveragePlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${pid}_${sample}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows_coveragePlot.png",' ||
       '                "selectiontag": "singlePlot",' ||
       '                "derivedFrom": "CoverageTextFile"' ||
       '            },' ||
       '            {' ||
       '                "class": "FastqcFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${fastx_qcOutputDirectory}/${dataSet}_${sample}_${run}_${lane}_${laneindex}_sequence_fastqc.zip",' ||
       '                "onMethod": "LaneFile.calcFastqc"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${fastx_qcOutputDirectory}/${dataSet}_${sample}_${run}_${lane}_${laneindex}_sequence_fastq_qcpass_status.txt",' ||
       '                "onMethod": "LaneFile.calcFastqc"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamIndexFile",' ||
       '                "pattern": "${sourcefile}.bai",' ||
       '                "derivedFrom": "BamFile"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${run}_${lane}_${cvalue,name=\"pairedBamSuffix\"}",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "FlagstatsFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
       '                "selectiontag": "extendedFlagstats",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamMetricsFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${sourcefileAtomic}.dupmark_metrics.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png_qcValues.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsizes.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "QCSummaryFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${sourcefileAtomicPrefix,delimiter=\"_\"}_${sourcefileProperty,type}_wroteQcSummary.txt",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
       '                "selectiontag": "genomeCoverage",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
       '                "selectiontag": "readBinsCoverage",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
       '                "selectiontag": "fingerprints",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/${run}_${lane}/qualitycontrol.json",' ||
       '                "selectiontag": "qcJson",' ||
       '                "onMethod": "LaneFileGroup.alignAndPairSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${pid}_${cvalue,name=\"defaultMergedBamSuffix\"}",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "FlagstatsFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
       '                "selectiontag": "extendedFlagstats",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamMetricsFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${sourcefileAtomic}.dupmark_metrics.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png_qcValues.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsizes.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_insertsize_plot.png",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "QCSummaryFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${sourcefileAtomicPrefix,delimiter=\"_\"}_${sourcefileProperty,type}_wroteQcSummary.txt",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}.DepthOfCoverage_Genome.txt",' ||
       '                "selectiontag": "genomeCoverage",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.txt",' ||
       '                "selectiontag": "readBinsCoverage",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/qualitycontrol.json",' ||
       '                "selectiontag": "qcJson",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamMetricsAlignmentSummaryFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${metricsOutputDirectory}/${cvalue,name=\"COLLECT_METRICS_PREFIX\"}.alignment_summary_metrics",' ||
       '                "onMethod": "BamFile.collectMetrics"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${fingerprintsOutputDirectory}/${sourcefileAtomic}.fp",' ||
       '                "selectiontag": "fingerprints",' ||
       '                "onMethod": "BamFileGroup.mergeAndRemoveDuplicatesSlim"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorOutputDirectory}/${sample}_${pid}.readCoverage_${cvalue,name=\"WINDOW_SIZE\",default=\"1\"}kb_windows.anno.txt.gz",' ||
       '                "selectiontag": "windowAnnotation",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.annotateCovWindows"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorOutputDirectory}/${sample}_${pid}_sex.txt",' ||
       '                "selectiontag": "sex",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.annotateCovWindows"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorOutputDirectory}/${sample}_${pid}_readCoverage_10kb_windows.filtered.txt.gz",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.mergeAndFilterCovWindows"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorOutputDirectory}/${sample}_${pid}_readCoverage_10kb_windows.filtered.corrected.txt.gz",' ||
       '                "selectiontag": "correctedWindows",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.correctGc"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorPlotOutputDirectory}/${sample}_${pid}_qc_gc_corrected.json",' ||
       '                "selectiontag": "tabSlim",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.correctGc"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorPlotOutputDirectory}/${sample}_${pid}_qc_gc_corrected.tsv",' ||
       '                "selectiontag": "tab",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.correctGc"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${gccorPlotOutputDirectory}/${sample}_${pid}_gc_corrected.png",' ||
       '                "selectiontag": "correctedGcPlot",' ||
       '                "onMethod": "de.dkfz.b080.co.methods.ACEseq.correctGc"' ||
       '            },' ||
       '            {' ||
       '                "class": "BamFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/${sample}_${pid}_${cvalue,name=\"TARGET_BAM_EXTENSION\",default=\"targetExtract.rmdup.bam\"}",' ||
       '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
       '            },' ||
       '            {' ||
       '                "class": "FlagstatsFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_flagstats.txt",' ||
       '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${flagstatsOutputDirectory}/${sourcefileAtomic}_extendedFlagstats.txt",' ||
       '                "selectiontag": "extendedFlagstats",' ||
       '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png_qcValues.txt",' ||
       '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.txt",' ||
       '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
       '            },' ||
       '            {' ||
       '                "class": "ChromosomeDiffPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${structuralVariationOutputDirectory}/${sourcefileAtomic}_DiffChroms.png",' ||
       '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesValueFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomic}_insertsize_plot.png_qcValues.txt",' ||
       '                "selectiontag": "targetExtract",' ||
       '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomic}_insertsizes.txt",' ||
       '                "selectiontag": "targetExtract",' ||
       '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
       '            },' ||
       '            {' ||
       '                "class": "InsertSizesPlotFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${insertSizesOutputDirectory}/${sourcefileAtomic}_insertsize_plot.png",' ||
       '                "selectiontag": "targetExtract",' ||
       '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomic}.DepthOfCoverage_Target.txt",' ||
       '                "selectiontag": "genomeCoverage",' ||
       '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
       '            },' ||
       '            {' ||
       '                "class": "CoverageTextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${coverageOutputDirectory}/${sourcefileAtomicPrefix,delimiter=\"_\"}_TargetsWithCov.txt",' ||
       '                "selectiontag": "targetsWithCoverage",' ||
       '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
       '            },' ||
       '            {' ||
       '                "class": "QCSummaryFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/${sourcefileAtomicPrefix,delimiter=\"_\"}_wroteQcSummary.txt",' ||
       '                "selectiontag": "targetExtract",' ||
       '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
       '            },' ||
       '            {' ||
       '                "class": "TextFile",' ||
       '                "pattern": "${outputAnalysisBaseDirectory}/qualitycontrol/merged/qualitycontrol_targetExtract.json",' ||
       '                "selectiontag": "qcJson",' ||
       '                "onMethod": "BamFile.extractTargetsCalculateCoverage"' ||
       '            }' ||
       '        ]' ||
       '    }' ||
       '}'
      )
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, selector_type, priority, external_workflow_config_fragment_id)
VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default values for PanCancer alignment EXON', 'DEFAULT_VALUES', 22, (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default values for PanCancer alignment EXON'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment EXON'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment EXON'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-202')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment EXON'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-204')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default values for PanCancer alignment EXON'), (SELECT id FROM seq_type WHERE name = 'EXON' AND single_cell = false AND library_layout = 'PAIRED')
ON CONFLICT DO NOTHING;
