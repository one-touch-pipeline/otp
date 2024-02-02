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

    changeSet(author: "Julian Rausch", id: "4685033775340-1") {
        renameColumn(oldColumnName: "pi_id", newColumnName: "granting_deputy_user_id", tableName: "piuser")
        renameColumn(oldColumnName: "deputypi_id", newColumnName: "deputy_user_id", tableName: "piuser")
        renameColumn(oldColumnName: "date_rights_granted", newColumnName: "date_deputy_granted", tableName: "piuser")
        renameTable(oldTableName: "piuser", newTableName: "deputy_relation")
    }
    changeSet(author: "Julian Rausch", id: "4685033775340-2") {
        dropIndex(tableName: "piuser", indexName: "pi_users_deputy_pi_idx")
        dropIndex(tableName: "piuser", indexName: "pi_users_pi_idx")
    }

    changeSet(author: "Julian Rausch", id: "4685033775340-3") {
        createIndex(tableName: "deputy_relation", indexName: "deputy_relation_deputy_user_idx") {
            column(name: "deputy_user_id")
        }
        createIndex(tableName: "deputy_relation", indexName: "deputy_relation_deputy_granting_user_idx") {
            column(name: "granting_deputy_user_id")
        }
    }



}
