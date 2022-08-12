
    INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default resources values for PanCancer alignment 1.2.182 EXON',
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
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default resources values for PanCancer alignment 1.2.182 EXON', 22, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default resources values for PanCancer alignment 1.2.182 EXON'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for PanCancer alignment 1.2.182 EXON'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for PanCancer alignment 1.2.182 EXON'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.182')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for PanCancer alignment 1.2.182 EXON'), (SELECT id FROM seq_type WHERE name = 'EXON' AND single_cell = false AND library_layout = 'PAIRED')
    ON CONFLICT DO NOTHING;
