databaseChangeLog = {

    changeSet(author: "", id: "1646408745950-1") {
        createTable(tableName: "workflow_version_selector") {
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

            column(name: "deprecation_date", type: "date")

            column(name: "seq_type_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "project_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "", id: "1646408745950-2") {
        addPrimaryKey(columnNames: "id", constraintName: "workflow_version_selectorPK", tableName: "workflow_version_selector")
    }

    changeSet(author: "", id: "1646408745950-5") {
        addForeignKeyConstraint(baseColumnNames: "project_id", baseTableName: "workflow_version_selector", constraintName: "FK9em5y65pma55fbmctwppw37e6", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project")
    }

    changeSet(author: "", id: "1646408745950-6") {
        addForeignKeyConstraint(baseColumnNames: "seq_type_id", baseTableName: "workflow_version_selector", constraintName: "FKm751s3l7h0a8hkqr9d3lhkdtm", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_type")
    }

    changeSet(author: "", id: "1646408745950-7") {
        addForeignKeyConstraint(baseColumnNames: "workflow_version_id", baseTableName: "workflow_version_selector", constraintName: "FKqbd60e12qp5r2p8ugxd1b9pkf", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_version")
    }

    changeSet(author: "", id: "1646408745950-8") {
        addForeignKeyConstraint(baseColumnNames: "previous_id", baseTableName: "workflow_version_selector", constraintName: "FKsnrrn3ni5l1bnb2i1tifs1ud0", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_version_selector")
    }

    changeSet(author: "", id: "1646408745950-9") {
        dropForeignKeyConstraint(baseTableName: "selected_project_seq_type_workflow_version", constraintName: "FK1u7ox5ktwedv5uvcgt9j8o0bl")
    }

    changeSet(author: "", id: "1646408745950-10") {
        dropForeignKeyConstraint(baseTableName: "selected_project_seq_type_workflow_version", constraintName: "FK6ywfo4l6v9tf93oracynlim2o")
    }

    changeSet(author: "", id: "1646408745950-11") {
        dropForeignKeyConstraint(baseTableName: "selected_project_seq_type_workflow_version", constraintName: "FKin6n213c2alv2x5po85y2ds5m")
    }

    changeSet(author: "", id: "1646408745950-16") {
        dropForeignKeyConstraint(baseTableName: "selected_project_seq_type_workflow_version", constraintName: "FKr0vxlruj2xsowvj2hr00dx97d")
    }

    changeSet(author: "", id: "1646408745950-31") {
        dropTable(tableName: "selected_project_seq_type_workflow_version")
    }
}
