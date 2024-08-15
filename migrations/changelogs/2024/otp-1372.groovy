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

    changeSet(author: "-", id: "1722860571348-90") {
        createTable(tableName: "script") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "scriptPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "script", type: "TEXT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "response", type: "TEXT")

            column(name: "last_updated", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "state", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
            column(name: "start_date", type: "TIMESTAMP WITH TIME ZONE")

            column(name: "end_date", type: "TIMESTAMP WITH TIME ZONE")

            column(name: "rerunnable", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "description", type: "TEXT")
        }
    }

    changeSet(author: "-", id: "1722860571348-91") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "script", constraintName: "FK6dimqdme5nhcjdp46dspl9wnm", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users", validate: "true")
    }
}
