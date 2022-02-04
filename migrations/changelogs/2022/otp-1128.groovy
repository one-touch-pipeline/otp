databaseChangeLog = {
    changeSet(author: "", id: "1645780657882-1") {
        createTable(tableName: "reference_genome_selector_species_with_strain") {
            column(name: "reference_genome_selector_species_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "species_with_strain_id", type: "BIGINT")
        }
    }
    changeSet(author: "", id: "1645780657882-4") {
        addForeignKeyConstraint(baseColumnNames: "species_with_strain_id", baseTableName: "reference_genome_selector_species_with_strain", constraintName: "FKdjtptiihx8m3ffjrcjkd3oct4", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_with_strain")
    }
    changeSet(author: "", id: "1645780657882-5") {
        addForeignKeyConstraint(baseColumnNames: "reference_genome_selector_species_id", baseTableName: "reference_genome_selector_species_with_strain", constraintName: "FKmuasj0s0haworrxl19djdrrm9", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "reference_genome_selector")
    }
    changeSet(author: "", id: "1645780657882-10") {
        dropForeignKeyConstraint(baseTableName: "reference_genome_selector", constraintName: "FKp61wgrd8fav5xwm21wtuwo4u9")
    }
    changeSet(author: "", id: "1645780657882-42") {
        dropColumn(columnName: "species_id", tableName: "reference_genome_selector")
    }
}
