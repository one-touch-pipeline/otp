/*
 * Copyright 2011-2024 The OTP authors
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

    changeSet(author: "Julian Rausch", id: "1689155155958-89") {
        createTable(tableName: "department_users") {
            column(name: "department_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "user_id", type: "BIGINT")
        }
    }

    changeSet(author: "Julian Rausch", id: "1689155155958-90") {
        addForeignKeyConstraint(baseColumnNames: "department_id", baseTableName: "department_users", constraintName: "FKj14w44ivguklhc8cbm9jmylee", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "department", validate: "true")
    }

    changeSet(author: "Julian Rausch", id: "1689155155958-91") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "department_users", constraintName: "FKnm5216tyo8yc9f7ir19hbpnsw", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users", validate: "true")
    }

    changeSet(author: "Julian Rausch", id: "1689155155958-92") {
        dropForeignKeyConstraint(baseTableName: "department", constraintName: "FK2h8c0g2j4rwyaumqibughbydx")
    }

    changeSet(author: "Julian Rausch", id: "1689155155958-128") {
        dropColumn(columnName: "department_head_id", tableName: "department")
    }
}
