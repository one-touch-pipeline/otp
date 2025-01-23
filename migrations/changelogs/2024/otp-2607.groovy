/*
 * Copyright 2011-2025 The OTP authors
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
    changeSet(author: "-", id: "1730972470301-113") {
        dropUniqueConstraint(constraintName: "bam_file_ega_alias_name_key", tableName: "bam_file_submission_object")
    }
    changeSet(author: "-", id: "1730972470301-114") {
        dropUniqueConstraint(constraintName: "data_file_ega_alias_name_key", tableName: "raw_sequence_file_submission_object")
    }
    changeSet(author: "-", id: "1730972470301-115") {
        dropUniqueConstraint(constraintName: "ega_alias_name_key", tableName: "sample_submission_object")
    }
    changeSet(author: "-", id: "1731305562384-92") {
        createIndex(indexName: "bam_file_submission_ega_alias_name_idx", tableName: "bam_file_submission_object") {
            column(name: "ega_alias_name")
        }
    }
    changeSet(author: "-", id: "1731305562384-93") {
        createIndex(indexName: "raw_sequence_file_submission_ega_alias_name_idx", tableName: "raw_sequence_file_submission_object") {
            column(name: "ega_alias_name")
        }
    }
    changeSet(author: "-", id: "1731305562384-94") {
        createIndex(indexName: "sample_submission_ega_alias_name_idx", tableName: "sample_submission_object") {
            column(name: "ega_alias_name")
        }
    }
}
