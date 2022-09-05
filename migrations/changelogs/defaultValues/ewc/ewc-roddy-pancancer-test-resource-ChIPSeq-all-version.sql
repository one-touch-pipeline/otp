
    INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default test-resource values for PanCancer alignment ChIP Seq test',
'{' ||
'    "RODDY": {' ||
'        "resources": {' ||
'            "alignAndPairSlim": {' ||
'                "value": "bwaMemSortSlim.sh",' ||
'                "walltime": "00:15:00",' ||
'                "basepath": "qcPipeline"' ||
'            },' ||
'            "coveragePlot": {' ||
'                "value": "genomeCoveragePlots.sh",' ||
'                "basepath": "qcPipeline"' ||
'            },' ||
'            "coveragePlotSingle": {' ||
'                "memory": "5",' ||
'                "value": "genomeCoveragePlots.sh",' ||
'                "walltime": "00:15:00",' ||
'                "basepath": "qcPipeline"' ||
'            }' ||
'        }' ||
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default test-resource values for PanCancer alignment ChIP Seq test', 100, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default test-resource values for PanCancer alignment ChIP Seq test'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default test-resource values for PanCancer alignment ChIP Seq test'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default test-resource values for PanCancer alignment ChIP Seq test'), (SELECT id FROM seq_type WHERE name = 'ChIP Seq' AND single_cell = false AND library_layout = 'PAIRED')
    ON CONFLICT DO NOTHING;
