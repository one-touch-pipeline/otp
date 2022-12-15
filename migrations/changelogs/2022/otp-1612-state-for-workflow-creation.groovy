/*
 * Copyright 2011-2022 The OTP authors
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

    changeSet(author: "", id: "1669813380810-76") {
        addColumn(tableName: "fastq_import_instance") {
            column(name: "state", type: "varchar(255)") {
                constraints(nullable: "true")
            }
        }
    }

    changeSet(author: "", id: "1669813380810-76b") {
        sql("update fastq_import_instance set state = 'SUCCESS';")
    }

    changeSet(author: "", id: "1669813380810-76c") {
        addNotNullConstraint(columnDataType: "varchar(255)", columnName: "state", tableName: "fastq_import_instance")
    }

    changeSet(author: "", id: "1669813380810-172") {
        createIndex(indexName: "fastq_import_instance_state_idx", tableName: "fastq_import_instance") {
            column(name: "state")
        }
    }
}
