databaseChangeLog = {

    changeSet(author: "klinga", id:"initial_database_schema") {
        sqlFile(path: 'changelogs/2018/initial_database_schema.sql')
    }
}
