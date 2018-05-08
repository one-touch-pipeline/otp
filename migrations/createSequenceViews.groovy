databaseChangeLog = {

    changeSet(author: "kaercher", id: "sequence view", runOnChange: "true") {
        sqlFile(path: 'changelogs/dbview/aggregate_sequences.sql')
        sqlFile(path: 'changelogs/dbview/sequences.sql')
    }
}