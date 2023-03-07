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

        changeSet(author: "strubelp", id: "1678154980771-82") {
            dropForeignKeyConstraint(baseTableName: "alignment_params", constraintName: "fk3dbeff6254649dfd")
        }
        changeSet(author: "strubelp", id: "1678154980771-83") {
            dropForeignKeyConstraint(baseTableName: "alignment_log", constraintName: "fk8b52cae8c38ea45b")
        }
        changeSet(author: "strubelp", id: "1678154980771-84") {
            dropForeignKeyConstraint(baseTableName: "alignment_log", constraintName: "fk8b52cae8f27d81e1")
        }
        changeSet(author: "strubelp", id: "1678154980771-85") {
            dropForeignKeyConstraint(baseTableName: "data_file", constraintName: "fkea50f8f11c055679")
        }
        changeSet(author: "strubelp", id: "1678154980771-100") {
            dropTable(tableName: "alignment_log")
        }
        changeSet(author: "strubelp", id: "1678154980771-101") {
            dropTable(tableName: "alignment_params")
        }
        changeSet(author: "strubelp", id: "1678154980771-118") {
            dropColumn(columnName: "alignment_log_id", tableName: "data_file")
        }
}