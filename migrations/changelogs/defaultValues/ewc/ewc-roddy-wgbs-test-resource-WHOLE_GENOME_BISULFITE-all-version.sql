
    INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default test-resource values for WGBS alignment WHOLE_GENOME_BISULFITE test',
'{' ||
'    "RODDY": {' ||
'        "resources": {' ||
'            "alignAndPairSlim": {' ||
'                "memory": "50",' ||
'                "value": "bwaMemSortSlimWithReadConversionForBisulfiteData.sh",' ||
'                "walltime": "1",' ||
'                "basepath": "bisulfiteWorkflow"' ||
'            }' ||
'        }' ||
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default test-resource values for WGBS alignment WHOLE_GENOME_BISULFITE test', 100, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default test-resource values for WGBS alignment WHOLE_GENOME_BISULFITE test'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default test-resource values for WGBS alignment WHOLE_GENOME_BISULFITE test'), (SELECT id FROM workflow WHERE name = 'WGBS alignment')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default test-resource values for WGBS alignment WHOLE_GENOME_BISULFITE test'), (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME_BISULFITE' AND single_cell = false AND library_layout = 'PAIRED')
    ON CONFLICT DO NOTHING;
