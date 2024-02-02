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
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 0, 'Default test-resource values for WGBS alignment test',
        '{' ||
        '    "RODDY": {' ||
        '        "resources": {' ||
        '            "cleanupScript": {' ||
        '                "value": "cleanupScript.sh",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "fastqc": {' ||
        '                "memory": "1",' ||
        '                "value": "checkFastQC.sh",' ||
        '                "walltime": "00:10:00",' ||
        '                "cores": 1,' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "alignment": {' ||
        '                "memory": "4",' ||
        '                "value": "bwaAlignSequence.sh",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "sampesort": {' ||
        '                "memory": "25",' ||
        '                "value": "bwaSampeSort.sh",' ||
        '                "walltime": "00:10:00",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "sampesortSlim": {' ||
        '                "memory": "25",' ||
        '                "value": "bwaSampeSortSlim.sh",' ||
        '                "walltime": "00:10:00",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "alignAndPair": {' ||
        '                "memory": "12",' ||
        '                "value": "bwaMemSort.sh",' ||
        '                "walltime": "00:10:00",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "samtoolsIndex": {' ||
        '                "value": "samtoolsIndexBamfile.sh",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "collectBamMetrics": {' ||
        '                "value": "picardCollectMetrics.sh",' ||
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
        '                "memory": "1",' ||
        '                "value": "differentiateChromosomes.sh",' ||
        '                "walltime": "1",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "genomeCoverage": {' ||
        '                "value": "genomeCoverage.sh",' ||
        '                "walltime": "1",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "readBinsCoverage": {' ||
        '                "value": "genomeCoverageReadBins.sh",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "coveragePlot": {' ||
        '                "value": "genomeCoveragePlots.sh",' ||
        '                "walltime": "00:15:00",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "coveragePlotSingle": {' ||
        '                "memory": "5",' ||
        '                "value": "genomeCoveragePlots.sh",' ||
        '                "walltime": "0:20:0",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "mergeAndRemoveDuplicates": {' ||
        '                "value": "mergeAndRemoveDuplicates.sh",' ||
        '                "walltime": "1",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "mergeAndRemoveDuplicatesSlimPicard": {' ||
        '                "memory": "50",' ||
        '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
        '                "walltime": "00:15:00",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "mergeAndRemoveDuplicatesSlimBioBambam": {' ||
        '                "memory": "5",' ||
        '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
        '                "walltime": "00:15:00",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "mergeAndRemoveDuplicatesSlimSambamba": {' ||
        '                "memory": "30",' ||
        '                "value": "mergeAndMarkOrRemoveDuplicatesSlim.sh",' ||
        '                "walltime": "00:15:00",' ||
        '                "basepath": "qcPipeline"' ||
        '            },' ||
        '            "methylationCalling": {' ||
        '                "value": "methylCtools_methylation_calling.sh",' ||
        '                "basepath": "bisulfiteWorkflow"' ||
        '            },' ||
        '            "methylationCallingMeta": {' ||
        '                "memory": "8",' ||
        '                "value": "methylCtools_methylation_calling_meta.sh",' ||
        '                "walltime": "00:15:00",' ||
        '                "cores": 3,' ||
        '                "basepath": "bisulfiteWorkflow"' ||
        '            }' ||
        '        }' ||
        '    }' ||
        '}')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 'Default test-resource values for WGBS alignment test', 100, 'DEFAULT_VALUES', (
    SELECT id FROM external_workflow_config_fragment WHERE name = 'Default test-resource values for WGBS alignment test'
                                                       AND deprecation_date IS NULL))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default test-resource values for WGBS alignment test'),
       (SELECT id FROM workflow WHERE name = 'WGBS alignment')
ON CONFLICT DO NOTHING;
