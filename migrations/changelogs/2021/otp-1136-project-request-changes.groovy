databaseChangeLog = {

    changeSet(author: "", id: "1625826223308-1") {
        createTable(tableName: "project_request_species_with_strain") {
            column(name: "project_request_species_with_strains_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "species_with_strain_id", type: "BIGINT")
        }
    }

    changeSet(author: "", id: "1625826223308-2") {
        createTable(tableName: "project_species_with_strain") {
            column(name: "project_species_with_strains_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "species_with_strain_id", type: "BIGINT")
        }
    }

    changeSet(author: "", id: "1625826223308-5") {
        addForeignKeyConstraint(baseColumnNames: "species_with_strain_id", baseTableName: "project_species_with_strain", constraintName: "FK5yay274o3ruc5mvx6ylit8hop", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_with_strain")
    }

    changeSet(author: "", id: "1625826223308-6") {
        addForeignKeyConstraint(baseColumnNames: "project_species_with_strains_id", baseTableName: "project_species_with_strain", constraintName: "FKai6vvaxbkge30u9ca5xwbd4uj", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project")
    }

    changeSet(author: "", id: "1625826223308-7") {
        addForeignKeyConstraint(baseColumnNames: "project_request_species_with_strains_id", baseTableName: "project_request_species_with_strain", constraintName: "FKbvi6kwe8sw97n826q7ls83kxm", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")
    }

    changeSet(author: "", id: "1625826223308-8") {
        addForeignKeyConstraint(baseColumnNames: "species_with_strain_id", baseTableName: "project_request_species_with_strain", constraintName: "FKqocijfvnd3ccgmp5y1h4pycp", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_with_strain")
    }

    changeSet(author: "", id: "1625826223308-9") {
        dropForeignKeyConstraint(baseTableName: "project_request", constraintName: "FKdxepnfeqyvtv3qldj7na2o9ql")
    }

    changeSet(author: "", id: "1625826223308-16") {
        dropForeignKeyConstraint(baseTableName: "project", constraintName: "fk_project__species_with_strain_id")
    }

    changeSet(author: "", id: "1625826223308-47") {
        dropColumn(columnName: "funding_body", tableName: "project")
    }

    changeSet(author: "", id: "1625826223308-48") {
        dropColumn(columnName: "funding_body", tableName: "project_request")
    }

    changeSet(author: "", id: "1625826223308-49") {
        dropColumn(columnName: "grant_id", tableName: "project")
    }

    changeSet(author: "", id: "1625826223308-50") {
        dropColumn(columnName: "grant_id", tableName: "project_request")
    }

    changeSet(author: "", id: "1625826223308-51") {
        dropColumn(columnName: "species_with_strain_id", tableName: "project")
    }

    changeSet(author: "", id: "1625826223308-52") {
        dropColumn(columnName: "species_with_strain_id", tableName: "project_request")
    }

    changeSet(author: "", id: "1628520226549-49") {
        dropColumn(columnName: "cost_center", tableName: "project")
    }

    changeSet(author: "", id: "1628520226549-50") {
        dropColumn(columnName: "cost_center", tableName: "project_request")
    }

    changeSet(author: "", id: "1628520226549-55") {
        dropColumn(columnName: "organizational_unit", tableName: "project")
    }

    changeSet(author: "", id: "1628520226549-56") {
        dropColumn(columnName: "organizational_unit", tableName: "project_request")
    }
}
