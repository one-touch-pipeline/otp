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

    changeSet(author: "klinga", id: "OTP-2862-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2862.sql')
    }

    changeSet(author: "wieset", id: "OTP-2964-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2964.sql')
    }

    changeSet(author: "klinga", id: "OTP-2863-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2863.sql')
    }

    changeSet(author: "wieset", id: "OTP-2965-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2965.sql')
    }

    changeSet(author: "wieset", id: "OTP-2966-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2966.sql')
    }

    changeSet(author: "kosnac", id: "OTP-2941-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2941.sql')
    }

    changeSet(author: "kosnac", id: "OTP-2991-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2991.sql')
    }

    changeSet(author: "wieset", id: "OTP-2967-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2967.sql')
    }

    changeSet(author: "strubelp", id: "OTP-2890-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2890.sql')
    }

    changeSet(author: "gruenj", id: "OTP-2878-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2878.sql')
    }

    changeSet(author: "strubelp", id: "OTP-2648-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2648.sql')
    }

}
