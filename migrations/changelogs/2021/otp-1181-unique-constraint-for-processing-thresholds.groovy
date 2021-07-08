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

    changeSet(author: "", id: "1625822316198-2") {
        sql("""
DELETE
FROM processing_thresholds
WHERE id IN (
    SELECT distinct processing_thresholds2.id
    FROM processing_thresholds processing_thresholds1,
         processing_thresholds processing_thresholds2
    WHERE processing_thresholds1.project_id = processing_thresholds2.project_id
      AND processing_thresholds1.seq_type_id = processing_thresholds2.seq_type_id
      AND processing_thresholds1.sample_type_id = processing_thresholds2.sample_type_id
      AND processing_thresholds1.id < processing_thresholds2.id
)
""")
    }

    changeSet(author: "", id: "1625822316198-3") {
        addUniqueConstraint(columnNames: "project_id, seq_type_id, sample_type_id", constraintName: "UKb7a01d2be3fdbf83d79c75252adb", tableName: "processing_thresholds")
    }

    changeSet(author: "", id: "1625822316198-4") {
        dropIndex(indexName: "processing_thresholds_project_seq_type_sample_type_idx", tableName: "processing_thresholds")
    }
}
