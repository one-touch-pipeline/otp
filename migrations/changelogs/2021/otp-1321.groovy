databaseChangeLog = {

    changeSet(author: "", id: "1637658550987-1") {
        createTable(tableName: "reference_genome_species_common_name") {
            column(name: "reference_genome_species_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "species_common_name_id", type: "BIGINT")
        }
    }

    changeSet(author: "", id: "1637658550987-4") {
        addForeignKeyConstraint(baseColumnNames: "species_common_name_id", baseTableName: "reference_genome_species_common_name", constraintName: "FK9hmtq6mrsfm6dopeufoso2662", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_common_name")
    }

    changeSet(author: "", id: "1637658550987-5") {
        addForeignKeyConstraint(baseColumnNames: "reference_genome_species_id", baseTableName: "reference_genome_species_common_name", constraintName: "FKe6ye0vk4yl5vhuc18egj5diyv", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "reference_genome")
    }
}
