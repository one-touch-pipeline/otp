
    INSERT INTO external_workflow_config_fragment(id, version, date_created, last_updated, object_version, name, config_values)
        VALUES(nextval('hibernate_sequence'), 0, now(), now(), 0, 'Default cvalue values for PanCancer alignment 1.2.73-1, 1.2.73-201, 1.2.73-204 WHOLE_GENOME',
'{' ||
'    "RODDY": {' ||
'        "cvalues": {' ||
'            "debugOptionsUseExitOnError": {' ||
'                "value": "false",' ||
'                "type": "boolean"' ||
'            }' ||
'        }' ||
'    }' ||
'}'
    )
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector(id, version, date_created, last_updated, name, priority, selector_type, external_workflow_config_fragment_id)
    VALUES(nextval('hibernate_sequence'), 0, now(), now(), 'Default cvalue values for PanCancer alignment 1.2.73-1, 1.2.73-201, 1.2.73-204 WHOLE_GENOME', 22, 'DEFAULT_VALUES', (
       SELECT id FROM external_workflow_config_fragment WHERE name = 'Default cvalue values for PanCancer alignment 1.2.73-1, 1.2.73-201, 1.2.73-204 WHOLE_GENOME'))
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow (external_workflow_config_selector_workflows_id, workflow_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for PanCancer alignment 1.2.73-1, 1.2.73-201, 1.2.73-204 WHOLE_GENOME'), (SELECT id FROM workflow WHERE name = 'PanCancer alignment')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for PanCancer alignment 1.2.73-1, 1.2.73-201, 1.2.73-204 WHOLE_GENOME'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-1')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for PanCancer alignment 1.2.73-1, 1.2.73-201, 1.2.73-204 WHOLE_GENOME'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-201')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_workflow_version (external_workflow_config_selector_workflow_versions_id, workflow_version_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for PanCancer alignment 1.2.73-1, 1.2.73-201, 1.2.73-204 WHOLE_GENOME'), (SELECT id FROM workflow_version WHERE workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND workflow_version.workflow_version = '1.2.73-204')
    ON CONFLICT DO NOTHING;

    INSERT INTO external_workflow_config_selector_seq_type (external_workflow_config_selector_seq_types_id, seq_type_id)
        SELECT (SELECT id FROM external_workflow_config_selector WHERE name = 'Default cvalue values for PanCancer alignment 1.2.73-1, 1.2.73-201, 1.2.73-204 WHOLE_GENOME'), (SELECT id FROM seq_type WHERE name = 'WHOLE_GENOME' AND single_cell = false AND library_layout = 'PAIRED')
    ON CONFLICT DO NOTHING;
