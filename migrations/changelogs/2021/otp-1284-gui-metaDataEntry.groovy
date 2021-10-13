/*
 * Copyright 2011-2021 The OTP authors
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
    changeSet(author: "", id: "1633688845318-1") {
        createTable(tableName: "species_common_name_import_alias") {
            column(name: "species_common_name_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "import_alias_string", type: "VARCHAR(255)")
        }
    }
    changeSet(author: "", id: "1633688845318-2") {
        addColumn(tableName: "species_common_name") {
            column(name: "legacy", type: "boolean")
        }
        update(tableName: "species_common_name"){
            column(name: "legacy", value: "false")
        }
    }
    changeSet(author: "", id: "1633688845318-5") {
        addForeignKeyConstraint(baseColumnNames: "species_common_name_id", baseTableName: "species_common_name_import_alias", constraintName: "FKpugomcewl8h4p3gdb6jk24jqa", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_common_name")
        addNotNullConstraint(tableName: "species_common_name", columnName: "legacy")
    }
}