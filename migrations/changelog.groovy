/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

    changeSet(author: "strubelp", id: "OTP-2822-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2822.sql')
    }

    changeSet(author: "gruenj", id: "remove-identifierInRunName-from-database-SQL") {
        sqlFile(path: 'changelogs/2018/remove-identifierInRunName-from-datadase.sql')
    }

    changeSet(author: "strubelp", id: "remove-unique-constraint-SQL") {
        sqlFile(path: 'changelogs/2018/remove-unique-constraint.sql')
    }

    changeSet(author: "borufka", id: "rename-COMMAND_ACTIVATION_RUN_YAPSA_PREFIX") {
        sqlFile(path: 'changelogs/2018/rename-COMMAND_ACTIVATION_RUN_YAPSA_PREFIX-to-COMMAND_ENABLE_MODULE.sql')
    }

    changeSet(author: "kosnac", id: "OTP-2899-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2899.sql')
    }

    changeSet(author: "wieset", id: "OTP-2968-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2968.sql')
    }

    changeSet(author: "klinga", id: "rename-columns-in-merging_criteria-SQL") {
        sqlFile(path: 'changelogs/2018/rename-columns-in-merging_criteria.sql')
    }

    changeSet(author: "strubelp", id: "index-on-datafile-md5sum") {
        sqlFile(path: 'changelogs/2018/index-on-datafile-md5sum.sql')
    }

    changeSet(author: "wieset", id: "rename-submission-to-egaSubmissin") {
        sqlFile(path: 'changelogs/2018/rename-submission-to-egaSubmissin.sql')
    }

    changeSet(author: "kosnac", id: "OTP-2984-SQL") {
        sqlFile(path: 'changelogs/2018/OTP-2984.sql')
    }

    changeSet(author: "strubelp", id: "OTP-3036-SQL") {
        sqlFile(path: 'changelogs/2019/OTP-3036.sql')
    }

    changeSet(author: "gruenj", id: "OTP-2784-SQL") {
        sqlFile(path: 'changelogs/2019/OTP-2784.sql')
    }

    changeSet(author: "kosnac", id: "OTP-3060-SQL") {
        sqlFile(path: 'changelogs/2019/OTP-3060.sql')
    }

    include file: 'changelogs/2019/OTP-3060-migrate-data.groovy'

    include file: 'changelogs/2019/OTP-3075.groovy'

    changeSet(author: "borufka", id: "OTP-3075-SQL") {
        sqlFile(path: 'changelogs/2019/OTP-3075.sql')
    }

    changeSet(author: "kosnac", id: "add-missing-null-constraints") {
        sqlFile(path: 'changelogs/2019/add-missing-null-constraints.sql')
    }
}
