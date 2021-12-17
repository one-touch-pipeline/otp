databaseChangeLog = {

    changeSet(author: "", id: "1638547608326-1") {
        createTable(tableName: "reference_genome_species_with_strain") {
            column(name: "reference_genome_species_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "species_with_strain_id", type: "BIGINT")
        }
    }

    changeSet(author: "", id: "1638547608326-2") {
        createTable(tableName: "sample_species_with_strain") {
            column(name: "sample_mixed_in_species_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "species_with_strain_id", type: "BIGINT")
        }
    }

    changeSet(author: "", id: "1638547608326-3") {
        createTable(tableName: "species_with_strain_import_alias") {
            column(name: "species_with_strain_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "import_alias_string", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "", id: "1638547608326-5") {
        addColumn(tableName: "reference_genome_selector") {
            column(name: "species_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "", id: "1638547608326-8") {
        addForeignKeyConstraint(baseColumnNames: "species_id", baseTableName: "individual", constraintName: "FK1ldcl6hj0qkgm63i0843srg0u", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_with_strain")
    }

    changeSet(author: "", id: "1638547608326-9") {
        addForeignKeyConstraint(baseColumnNames: "species_with_strain_id", baseTableName: "species_with_strain_import_alias", constraintName: "FKhplsh5fhlj4pnlqqig85hdndg", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_with_strain")
    }

    changeSet(author: "", id: "1638547608326-10") {
        addForeignKeyConstraint(baseColumnNames: "sample_mixed_in_species_id", baseTableName: "sample_species_with_strain", constraintName: "FKjofb5pxjfkcl2cvbutgn9a13e", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "sample")
    }

    changeSet(author: "", id: "1638547608326-11") {
        addForeignKeyConstraint(baseColumnNames: "reference_genome_species_id", baseTableName: "reference_genome_species_with_strain", constraintName: "FKjqycwyqpntsq2hr69w7ugq1tg", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "reference_genome")
    }

    changeSet(author: "", id: "1638547608326-12") {
        addForeignKeyConstraint(baseColumnNames: "species_with_strain_id", baseTableName: "reference_genome_species_with_strain", constraintName: "FKnh6r0yntwr2v2xqqoqpgc3uig", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_with_strain")
    }

    changeSet(author: "", id: "1638547608326-13") {
        addForeignKeyConstraint(baseColumnNames: "species_with_strain_id", baseTableName: "sample_species_with_strain", constraintName: "FKodg8l0hs3mtu1ivpdq56ec45k", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_with_strain")
    }

    changeSet(author: "", id: "1638547608326-14") {
        addForeignKeyConstraint(baseColumnNames: "species_id", baseTableName: "reference_genome_selector", constraintName: "FKp61wgrd8fav5xwm21wtuwo4u9", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_with_strain")
    }

    changeSet(author: "", id: "1638547608326-15") {
        dropForeignKeyConstraint(baseTableName: "sample_species_common_name", constraintName: "FK5fg3qarv4hcpm38pr20y3dk6t")
    }

    changeSet(author: "", id: "1638547608326-16") {
        dropForeignKeyConstraint(baseTableName: "reference_genome_selector", constraintName: "FK6yqetv51kleoaav4fmiguke4u")
    }

    changeSet(author: "", id: "1638547608326-17") {
        dropForeignKeyConstraint(baseTableName: "individual", constraintName: "FK9aay5ppxvjq6r4g6pgvljywmb")
    }

    changeSet(author: "", id: "1638547608326-18") {
        dropForeignKeyConstraint(baseTableName: "reference_genome_species_common_name", constraintName: "FK9hmtq6mrsfm6dopeufoso2662")
    }

    changeSet(author: "", id: "1638547608326-19") {
        dropForeignKeyConstraint(baseTableName: "reference_genome_species_common_name", constraintName: "FKe6ye0vk4yl5vhuc18egj5diyv")
    }

    changeSet(author: "", id: "1638547608326-24") {
        dropForeignKeyConstraint(baseTableName: "sample_species_common_name", constraintName: "FKn4kciqm1tlr9pdxkawpy7sk4p")
    }

    changeSet(author: "", id: "1638547608326-25") {
        dropForeignKeyConstraint(baseTableName: "species_common_name_import_alias", constraintName: "FKpugomcewl8h4p3gdb6jk24jqa")
    }

    changeSet(author: "", id: "1638547608326-40") {
        dropTable(tableName: "reference_genome_species_common_name")
    }

    changeSet(author: "", id: "1638547608326-41") {
        dropTable(tableName: "sample_species_common_name")
    }

    changeSet(author: "", id: "1638547608326-42") {
        dropTable(tableName: "species_common_name_import_alias")
    }

    changeSet(author: "", id: "1638547608326-60") {
        dropColumn(columnName: "legacy", tableName: "species_common_name")
    }

    changeSet(author: "", id: "1638547608326-61") {
        dropColumn(columnName: "species_common_name_id", tableName: "reference_genome_selector")
    }
}
