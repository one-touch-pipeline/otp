databaseChangeLog = {

    changeSet(author: "Nico Langhorst", id: "1633601557543-1") {
        createTable(tableName: "sample_species_common_name") {
            column(name: "sample_mixed_in_species_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "species_common_name_id", type: "BIGINT")
        }
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-2") {
        createTable(tableName: "species_common_name") {
            column(name: "id", type: "BIGINT")
            column(name: "version", type: "BIGINT")
            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE")
            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE")
            column(name: "name", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-3") {
        addColumn(tableName: "species") {
            column(name: "species_common_name_id", type: "int8")
        }
        update(tableName: "species") {
            column(name: "species_common_name_id", valueComputed:"common_name_id")
        }
        addNotNullConstraint(tableName: "species", columnName: "species_common_name_id")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-4") {
        addPrimaryKey(columnNames: "id", constraintName: "species_common_namePK", tableName: "species_common_name")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-125456454"){
        sql("""
            INSERT INTO species_common_name(id, version, name, date_created, last_updated)
            SELECT id, version, name, date_created, last_updated
            FROM common_name;
        """)
        addNotNullConstraint(tableName: "species_common_name", columnName: "id")
        addNotNullConstraint(tableName: "species_common_name", columnName: "version")
        addNotNullConstraint(tableName: "species_common_name", columnName: "name")
        addNotNullConstraint(tableName: "species_common_name", columnName: "last_updated")
        addNotNullConstraint(tableName: "species_common_name", columnName: "date_created")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-7") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_SPECIES_COMMON_NAMENAME_COL", tableName: "species_common_name")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-8") {
        addForeignKeyConstraint(baseColumnNames: "species_common_name_id", baseTableName: "species", constraintName: "FK43n3q0scwt2bwpdlgii02iafi", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_common_name")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-9") {
        addForeignKeyConstraint(baseColumnNames: "sample_mixed_in_species_id", baseTableName: "sample_species_common_name", constraintName: "FK5fg3qarv4hcpm38pr20y3dk6t", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "sample")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-10") {
        addForeignKeyConstraint(baseColumnNames: "species_id", baseTableName: "individual", constraintName: "FK9aay5ppxvjq6r4g6pgvljywmb", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_common_name")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-11") {
        addForeignKeyConstraint(baseColumnNames: "species_common_name_id", baseTableName: "sample_species_common_name", constraintName: "FKn4kciqm1tlr9pdxkawpy7sk4p", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_common_name")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-12") {
        dropForeignKeyConstraint(baseTableName: "individual", constraintName: "FK1l5rx680lr3qe4ir3acc0hdgf")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-13") {
        dropForeignKeyConstraint(baseTableName: "sample_common_name", constraintName: "FKar1okxqf8tj1k6ff2q6qsaf4a")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-18") {
        dropForeignKeyConstraint(baseTableName: "sample_common_name", constraintName: "FKrdyf0yckeip1htcaed7ue0x3y")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-20") {
        dropForeignKeyConstraint(baseTableName: "species", constraintName: "fk_species__common_name_id")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-30") {
        dropUniqueConstraint(constraintName: "common_name_name_key", tableName: "common_name")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-35") {
        dropTable(tableName: "common_name")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-36") {
        dropTable(tableName: "sample_common_name")
    }

    changeSet(author: "Nico Langhorst", id: "1633601557543-54") {
        dropColumn(columnName: "common_name_id", tableName: "species")
    }
}
