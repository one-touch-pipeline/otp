/*
 * Copyright 2011-2026 The OTP authors
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
    changeSet(author: "foued", id: "otp-3046-2") {
        createTable(tableName: "sequence_cram_file_fastq_file") {
            column(name: "sequence_cram_file_source_fastq_files_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "fastq_file_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "foued", id: "otp-3046-3") {
        addForeignKeyConstraint(
                constraintName: "sequence_cram_file_fastq_file_sequence_cram_file_fkey",
                baseTableName: "sequence_cram_file_fastq_file",
                baseColumnNames: "sequence_cram_file_source_fastq_files_id",
                referencedTableName: "raw_sequence_file",
                referencedColumnNames: "id"
        )
        addForeignKeyConstraint(
                constraintName: "sequence_cram_file_fastq_file_fastq_file_fkey",
                baseTableName: "sequence_cram_file_fastq_file",
                baseColumnNames: "fastq_file_id",
                referencedTableName: "raw_sequence_file",
                referencedColumnNames: "id"
        )
    }

    changeSet(author: "foued", id: "otp-3046-4") {
        createIndex(indexName: "sequence_cram_file_fastq_file_sequence_cram_file_idx", tableName: "sequence_cram_file_fastq_file") {
            column(name: "sequence_cram_file_source_fastq_files_id")
        }
        createIndex(indexName: "sequence_cram_file_fastq_file_fastq_file_idx", tableName: "sequence_cram_file_fastq_file") {
            column(name: "fastq_file_id")
        }
    }
}
