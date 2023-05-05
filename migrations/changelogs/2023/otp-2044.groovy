/*
 * Copyright 2011-2023 The OTP authors
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

    changeSet(author: "-", id: "1680576459688-92") {
        addColumn(tableName: "wes_log") {
            column(name: "task_logs_idx", type: "int4")
        }
    }

    changeSet(author: "-", id: "1680576459688-93") {
        addColumn(tableName: "wes_log") {
            column(name: "wes_run_log_id", type: "int8")
        }
    }

    changeSet(author: "-", id: "1680576459688-96") {
        addForeignKeyConstraint(baseColumnNames: "wes_run_log_id", baseTableName: "wes_log", constraintName: "FKffdx2dvrplxc515o3dujd0pvr", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "wes_run_log", validate: "true")
    }

    changeSet(author: "-", id: "1680576459688-101") {
        dropForeignKeyConstraint(baseTableName: "wes_run_log_wes_log", constraintName: "FKogq6dnt82emgwdsdt4srcbjkn")
    }

    changeSet(author: "-", id: "1680576459688-117") {
        dropTable(tableName: "wes_run_log_wes_log")
    }

    changeSet(author: "-", id: "1680576459688-34") {
        dropNotNullConstraint(columnDataType: "bigint", columnName: "run_log_id", tableName: "wes_run_log")
    }
}
