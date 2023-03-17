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
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 0, 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204',
        '{' ||
        '    "RODDY": {' ||
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
        '            "methylationCalling": {' ||
        '                "value": "methylCtools_methylation_calling.sh",' ||
        '                "basepath": "bisulfiteWorkflow"' ||
        '            },' ||
        '            "methylationCallingMeta": {' ||
        '                "memory": "20",' ||
        '                "value": "methylCtools_methylation_calling_meta.sh",' ||
        '                "nodes": 1,' ||
        '                "walltime": "120",' ||
        '                "cores": 13,' ||
        '                "basepath": "bisulfiteWorkflow"' ||
        '            }' ||
        '        }' ||
        '    }' ||
        '}')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
VALUES (NEXTVAL('hibernate_sequence'), 0, NOW(), NOW(), 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204', 6,
        'DEFAULT_VALUES', (
            SELECT id
            FROM external_workflow_config_fragment
            WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'))
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'),
       (SELECT id FROM workflow WHERE name = 'WGBS alignment')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'),
       (SELECT id
        FROM workflow_version
        WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND workflow_version.workflow_version = '1.2.73-1')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'),
       (SELECT id
        FROM workflow_version
        WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND workflow_version.workflow_version = '1.2.73-2')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'),
       (SELECT id
        FROM workflow_version
        WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND workflow_version.workflow_version = '1.2.73-201')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'),
       (SELECT id
        FROM workflow_version
        WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND workflow_version.workflow_version = '1.2.73-202')
ON CONFLICT DO NOTHING;

INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
SELECT (SELECT id
        FROM external_workflow_config_selector
        WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204'),
       (SELECT id
        FROM workflow_version
        WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND workflow_version.workflow_version = '1.2.73-204')
ON CONFLICT DO NOTHING;
