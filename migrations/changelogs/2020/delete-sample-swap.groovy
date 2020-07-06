databaseChangeLog = {
    changeSet(author: "", id: "1593677545502-1") {
        dropForeignKeyConstraint(baseTableName: "swap_info_seq_track", constraintName: "swap_info_seq_track_swap_info_seq_tracks_id_fkey")
    }

    changeSet(author: "", id: "1593677545502-8") {
        dropForeignKeyConstraint(baseTableName: "file_system_changes", constraintName: "file_system_changes_swap_info_id_fkey")
    }

    changeSet(author: "", id: "1593677545502-17") {
        dropForeignKeyConstraint(baseTableName: "swap_info", constraintName: "swap_info_user_id_fkey")
    }

    changeSet(author: "", id: "1593677545502-26") {
        dropTable(tableName: "file_system_changes")
    }

    changeSet(author: "", id: "1593677545502-27") {
        dropTable(tableName: "swap_info")
    }

    changeSet(author: "", id: "1593677545502-28") {
        dropTable(tableName: "swap_info_seq_track")
    }
}
