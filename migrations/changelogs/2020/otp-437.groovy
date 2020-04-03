databaseChangeLog = {

    changeSet(author: "m139l (generated)", id: "1586959652150-1") {
        createTable(tableName: "active_project_workflow") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "object_version", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "workflow_version_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "previous_id", type: "BIGINT")

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "active", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "deprecation_date", type: "date") {
                constraints(nullable: "false")
            }

            column(name: "seq_type_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "project_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-2") {
        createTable(tableName: "external_workflow_config_fragment") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "object_version", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "previous_id", type: "BIGINT")

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "deprecation_date", type: "date") {
                constraints(nullable: "false")
            }

            column(name: "comment_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "config_values", type: "TEXT") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-3") {
        createTable(tableName: "external_workflow_config_selector") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "fine_tuning_priority", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "base_priority", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "external_workflow_config_fragment_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-4") {
        createTable(tableName: "external_workflow_config_selector_library_preparation_kit") {
            column(name: "external_workflow_config_selector_library_preparation_kits_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "library_preparation_kit_id", type: "BIGINT")
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-5") {
        createTable(tableName: "external_workflow_config_selector_project") {
            column(name: "external_workflow_config_selector_projects_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "project_id", type: "BIGINT")
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-6") {
        createTable(tableName: "external_workflow_config_selector_reference_genome") {
            column(name: "external_workflow_config_selector_reference_genomes_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "reference_genome_id", type: "BIGINT")
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-7") {
        createTable(tableName: "external_workflow_config_selector_seq_type") {
            column(name: "external_workflow_config_selector_seq_types_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "seq_type_id", type: "BIGINT")
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-8") {
        createTable(tableName: "external_workflow_config_selector_workflow") {
            column(name: "external_workflow_config_selector_workflows_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "workflow_id", type: "BIGINT")
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-9") {
        createTable(tableName: "external_workflow_config_selector_workflow_version") {
            column(name: "external_workflow_config_selector_workflow_versions_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "workflow_version_id", type: "BIGINT")
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-10") {
        createTable(tableName: "reference_genome_selector") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "reference_genome_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "specific_reference_genome", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-11") {
        createTable(tableName: "reference_genome_selector_active_project_workflow") {
            column(name: "reference_genome_selector_active_project_workflows_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "active_project_workflow_id", type: "BIGINT")
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-12") {
        createTable(tableName: "workflow_reference_genome") {
            column(name: "workflow_allowed_reference_genomes_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "reference_genome_id", type: "BIGINT")
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-13") {
        createTable(tableName: "workflow_run_external_workflow_config_fragment") {
            column(name: "workflow_run_configs_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "external_workflow_config_fragment_id", type: "BIGINT")

            column(name: "configs_idx", type: "INT")
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-14") {
        createTable(tableName: "workflow_seq_type") {
            column(name: "workflow_supported_seq_types_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "seq_type_id", type: "BIGINT")
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-15") {
        createTable(tableName: "workflow_version") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "version", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "workflow_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-16") {
        addPrimaryKey(columnNames: "id", constraintName: "active_project_workflowPK", tableName: "active_project_workflow")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-17") {
        addPrimaryKey(columnNames: "id", constraintName: "external_workflow_config_fragmentPK", tableName: "external_workflow_config_fragment")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-18") {
        addPrimaryKey(columnNames: "id", constraintName: "external_workflow_config_selectorPK", tableName: "external_workflow_config_selector")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-19") {
        addPrimaryKey(columnNames: "id", constraintName: "reference_genome_selectorPK", tableName: "reference_genome_selector")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-20") {
        addPrimaryKey(columnNames: "id", constraintName: "workflow_versionPK", tableName: "workflow_version")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-23") {
        addForeignKeyConstraint(baseColumnNames: "project_id", baseTableName: "external_workflow_config_selector_project", constraintName: "FK7dfljpx4fmy14113gknnnvsak", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-24") {
        addForeignKeyConstraint(baseColumnNames: "workflow_version_id", baseTableName: "external_workflow_config_selector_workflow_version", constraintName: "FK8dissi4rwptvv84cp7gdon3ld", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_version")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-25") {
        addForeignKeyConstraint(baseColumnNames: "workflow_version_id", baseTableName: "active_project_workflow", constraintName: "FK95jki3fxnn1vuhd8aw1nwx611", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_version")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-26") {
        addForeignKeyConstraint(baseColumnNames: "external_workflow_config_selector_workflow_versions_id", baseTableName: "external_workflow_config_selector_workflow_version", constraintName: "FK9lw1h4w9cp3na1s34uiwu2f5t", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "external_workflow_config_selector")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-27") {
        addForeignKeyConstraint(baseColumnNames: "reference_genome_id", baseTableName: "reference_genome_selector", constraintName: "FKbymwu5t4ajsm10gtqdo295j1x", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "reference_genome")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-28") {
        addForeignKeyConstraint(baseColumnNames: "external_workflow_config_selector_library_preparation_kits_id", baseTableName: "external_workflow_config_selector_library_preparation_kit", constraintName: "FKcv03ofbaf0j6lhhin5nyy1fep", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "external_workflow_config_selector")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-29") {
        addForeignKeyConstraint(baseColumnNames: "workflow_allowed_reference_genomes_id", baseTableName: "workflow_reference_genome", constraintName: "FKdt5yxfdx2f93cs61ft48v9b6w", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-30") {
        addForeignKeyConstraint(baseColumnNames: "reference_genome_id", baseTableName: "external_workflow_config_selector_reference_genome", constraintName: "FKej0xflqol53cg0yok9kj0671q", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "reference_genome")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-31") {
        addForeignKeyConstraint(baseColumnNames: "external_workflow_config_selector_reference_genomes_id", baseTableName: "external_workflow_config_selector_reference_genome", constraintName: "FKemd1ga88oa1krw8sb9sopn39f", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "external_workflow_config_selector")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-32") {
        addForeignKeyConstraint(baseColumnNames: "comment_id", baseTableName: "external_workflow_config_fragment", constraintName: "FKfh915bfvqclb0wh281k7abrpd", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "comment")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-33") {
        addForeignKeyConstraint(baseColumnNames: "external_workflow_config_fragment_id", baseTableName: "external_workflow_config_selector", constraintName: "FKfwuhqgqmdojj02p8ip5ivveao", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "external_workflow_config_fragment")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-34") {
        addForeignKeyConstraint(baseColumnNames: "workflow_id", baseTableName: "workflow_version", constraintName: "FKgjhy6w5q8buydj1hboch9dxl5", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-35") {
        addForeignKeyConstraint(baseColumnNames: "project_id", baseTableName: "active_project_workflow", constraintName: "FKhmo2tg9yp076rdr4jw657stin", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-36") {
        addForeignKeyConstraint(baseColumnNames: "reference_genome_selector_active_project_workflows_id", baseTableName: "reference_genome_selector_active_project_workflow", constraintName: "FKhww4kaejsak9wqibahvuofcwl", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "reference_genome_selector")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-37") {
        addForeignKeyConstraint(baseColumnNames: "previous_id", baseTableName: "active_project_workflow", constraintName: "FKhxyrw6ba20n0orcxjnr6xr9yh", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "active_project_workflow")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-38") {
        addForeignKeyConstraint(baseColumnNames: "library_preparation_kit_id", baseTableName: "external_workflow_config_selector_library_preparation_kit", constraintName: "FKj8qu0ub20hbgpcx10mjey7pen", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "library_preparation_kit")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-39") {
        addForeignKeyConstraint(baseColumnNames: "workflow_id", baseTableName: "external_workflow_config_selector_workflow", constraintName: "FKjn7vkp6lnyq3y5ry9dwov0v1h", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-40") {
        addForeignKeyConstraint(baseColumnNames: "seq_type_id", baseTableName: "workflow_seq_type", constraintName: "FKjvsn4l5v6lirixq0i9jv57obj", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_type")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-41") {
        addForeignKeyConstraint(baseColumnNames: "reference_genome_id", baseTableName: "workflow_reference_genome", constraintName: "FKk7iras1xy5c9veghgbndrybxq", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "reference_genome")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-42") {
        addForeignKeyConstraint(baseColumnNames: "external_workflow_config_selector_seq_types_id", baseTableName: "external_workflow_config_selector_seq_type", constraintName: "FKlk5kih0j8my884aobmo8fphap", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "external_workflow_config_selector")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-43") {
        addForeignKeyConstraint(baseColumnNames: "external_workflow_config_fragment_id", baseTableName: "workflow_run_external_workflow_config_fragment", constraintName: "FKmtrmx711o92jkvito9syjsyb6", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "external_workflow_config_fragment")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-44") {
        addForeignKeyConstraint(baseColumnNames: "active_project_workflow_id", baseTableName: "reference_genome_selector_active_project_workflow", constraintName: "FKnprkx4jcx82515eq7p6xye2d4", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "active_project_workflow")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-45") {
        addForeignKeyConstraint(baseColumnNames: "previous_id", baseTableName: "external_workflow_config_fragment", constraintName: "FKo6gbflkmf1snoouep44se0dcw", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "external_workflow_config_fragment")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-46") {
        addForeignKeyConstraint(baseColumnNames: "external_workflow_config_selector_workflows_id", baseTableName: "external_workflow_config_selector_workflow", constraintName: "FKo78141440cns5kp2fbxxdaja2", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "external_workflow_config_selector")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-47") {
        addForeignKeyConstraint(baseColumnNames: "seq_type_id", baseTableName: "active_project_workflow", constraintName: "FKoiobjb5mn07vacpvdvtsk03xr", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_type")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-48") {
        addForeignKeyConstraint(baseColumnNames: "workflow_supported_seq_types_id", baseTableName: "workflow_seq_type", constraintName: "FKpgv079b145o8wxaikfutjn2pb", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-49") {
        addForeignKeyConstraint(baseColumnNames: "external_workflow_config_selector_projects_id", baseTableName: "external_workflow_config_selector_project", constraintName: "FKqljoam7w0pu6pm67e1abtuw7y", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "external_workflow_config_selector")
    }

    changeSet(author: "m139l (generated)", id: "1586959652150-50") {
        addForeignKeyConstraint(baseColumnNames: "seq_type_id", baseTableName: "external_workflow_config_selector_seq_type", constraintName: "FKr74u0p64hvmhjgd9m3fx4k45f", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_type")
    }
}
