
    INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204, 1.2.73-3 WHOLE_GENOME_BISULFITE_TAGMENTATION',
'{' ||
'    "RODDY": {' ||
'        "resources": {' ||
'            "alignAndPairSlim": {' ||
'                "memory": "60",' ||
'                "value": "bwaMemSortSlimWithReadConversionForBisulfiteData.sh",' ||
'                "nodes": 1,' ||
'                "walltime": "240",' ||
'                "cores": 14,' ||
'                "basepath": "bisulfiteWorkflow"' ||
'            }' ||
'        }' ||
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204, 1.2.73-3 WHOLE_GENOME_BISULFITE_TAGMENTATION', 22, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204, 1.2.73-3 WHOLE_GENOME_BISULFITE_TAGMENTATION'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204, 1.2.73-3 WHOLE_GENOME_BISULFITE_TAGMENTATION'), (SELECT id FROM workflow WHERE name = 'WGBS alignment')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204, 1.2.73-3 WHOLE_GENOME_BISULFITE_TAGMENTATION'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND workflow_version.workflow_version = '1.2.73-1')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204, 1.2.73-3 WHOLE_GENOME_BISULFITE_TAGMENTATION'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND workflow_version.workflow_version = '1.2.73-2')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204, 1.2.73-3 WHOLE_GENOME_BISULFITE_TAGMENTATION'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND workflow_version.workflow_version = '1.2.73-201')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204, 1.2.73-3 WHOLE_GENOME_BISULFITE_TAGMENTATION'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND workflow_version.workflow_version = '1.2.73-202')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204, 1.2.73-3 WHOLE_GENOME_BISULFITE_TAGMENTATION'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND workflow_version.workflow_version = '1.2.73-204')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204, 1.2.73-3 WHOLE_GENOME_BISULFITE_TAGMENTATION'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND workflow_version.workflow_version = '1.2.73-3')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default resources values for WGBS alignment 1.2.73-1, 1.2.73-2, 1.2.73-201, 1.2.73-202, 1.2.73-204, 1.2.73-3 WHOLE_GENOME_BISULFITE_TAGMENTATION'), (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME_BISULFITE_TAGMENTATION' AND single_cell = false AND library_layout = 'PAIRED')
    ON CONFLICT DO NOTHING;
