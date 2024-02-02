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

    changeSet(author: "Alma Runde", id: "otp-2158-1") {
        addColumn(tableName: "individual") {
            column(name: "uuid", type: "UUID") {
                constraints(nullable: "true")
            }
        }
    }

    changeSet(author: "Alma Runde", id: "otp-2158-2") {
        sql("update individual set uuid = gen_random_uuid()")
    }

    changeSet(author: "Alma Runde", id: "otp-2158-3") {
        addNotNullConstraint(columnDataType: "UUID", columnName: "uuid", tableName: "individual")
    }

    changeSet(author: "Alma Runde", id: "otp-2158-4") {
        createIndex(indexName: "individual_uuid_idx", tableName: "individual") {
            column(name: "uuid")
        }
    }

    changeSet(author: "Alma Runde", id: "otp-2158-5") {
        addUniqueConstraint(columnNames: "uuid", constraintName: "_INDIVIDUALUUID_COL", tableName: "individual")
    }
}
