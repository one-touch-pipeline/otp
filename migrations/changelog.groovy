databaseChangeLog = {

    changeSet(author: "klinga", id: "initial_database_schema") {
        sqlFile(path: 'changelogs/2018/initial_database_schema.sql')
    }

    changeSet(author: "kosnac", id: "MWP-ST-NULL-CONSTRAINT") {
        sqlFile(path: 'changelogs/2018/null-constraint-on-mwp-st.sql')
    }

    changeSet(author: "wieset", id: "OTP-2882-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2882.sql')
    }

    changeSet(author: "strubelp", id: "OTP-2879-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2879.sql')
    }

    changeSet(author: "strubelp", id: "OTP-2880-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2880.sql')
    }
}
