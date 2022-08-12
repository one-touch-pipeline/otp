
    INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default test-resource values for PanCancer alignment  WHOLE_GENOME test',
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
'            "alignAndPairSlim": {' ||
'                "memory": "12",' ||
'                "value": "bwaMemSortSlim.sh",' ||
'                "walltime": "00:15:00",' ||
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
'                "walltime": "0:20:00",' ||
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
'            }' ||
'        }' ||
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default test-resource values for PanCancer alignment  WHOLE_GENOME test', 100, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default test-resource values for PanCancer alignment  WHOLE_GENOME test'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default test-resource values for PanCancer alignment  WHOLE_GENOME test'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default test-resource values for PanCancer alignment  WHOLE_GENOME test'), (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME' AND single_cell = false AND library_layout = 'PAIRED')
    ON CONFLICT DO NOTHING;
