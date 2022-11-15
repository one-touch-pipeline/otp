databaseChangeLog = {

    changeSet(author: "", id: "1668445515644-76") {
        createTable(tableName: "reference_genome_species") {
            column(name: "reference_genome_species_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "species_id", type: "BIGINT")
        }
    }

    changeSet(author: "", id: "1668445515644-172") {
        addForeignKeyConstraint(baseColumnNames: "species_id", baseTableName: "reference_genome_species", constraintName: "FKcos3fbb27hx4r5p0j185gqwu9", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species", validate: "true")
    }

    changeSet(author: "", id: "1668445515644-173") {
        addForeignKeyConstraint(baseColumnNames: "reference_genome_species_id", baseTableName: "reference_genome_species", constraintName: "FKs3jmeh2c4y3vp0slw10afnbds", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "reference_genome", validate: "true")
    }
}
