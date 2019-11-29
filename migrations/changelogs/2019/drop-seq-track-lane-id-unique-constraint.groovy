databaseChangeLog = {
    changeSet(author: "kosnac (generated)", id: "1575022029733-14") {
        dropUniqueConstraint(constraintName: "seq_track_cell_position_run_id_laneid_key", tableName: "seq_track")
    }
}
