databaseChangeLog = {

    changeSet(author: "borufka", id: "1614251113228-38") {
        dropColumn(columnName: "deletion_date", tableName: "abstract_bam_file")
    }

    changeSet(author: "borufka", id: "1614251113228-39") {
        dropColumn(columnName: "deletion_date", tableName: "processed_sai_file")
    }
}
