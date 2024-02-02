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
    changeSet(author: "-", id: "1697796529996-90") {
        addColumn(tableName: "abstract_quality_assessment") {
            column(name: "abstract_bam_file_id", type: "int8")
        }
    }

    changeSet(author: "-", id: "1697796529996-91") {
        addForeignKeyConstraint(baseColumnNames: "abstract_bam_file_id", baseTableName: "abstract_quality_assessment", constraintName: "FKl7mpibp05r1vtkwajlc6cy9u2", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "abstract_bam_file", validate: "true")
    }

    changeSet(author: "-", id: "1697796529996-92") {
        sql("""
            UPDATE abstract_quality_assessment SET abstract_bam_file_id = (
                SELECT abstract_bam_file_id FROM quality_assessment_merged_pass 
                                            WHERE abstract_quality_assessment.quality_assessment_merged_pass_id = quality_assessment_merged_pass.id)
        """)
    }

    changeSet(author: "-", id: "1697796529996-97") {
        dropForeignKeyConstraint(baseTableName: "abstract_quality_assessment", constraintName: "fk4525553f40bbb6dc")
    }

    changeSet(author: "-", id: "1697796529996-98") {
        dropForeignKeyConstraint(baseTableName: "quality_assessment_merged_pass", constraintName: "fke70b46871a932a30")
    }

    changeSet(author: "-", id: "1697796529996-113") {
        dropUniqueConstraint(constraintName: "uc_quality_assessment_merged_passabstract_merged_bam_file_id_co", tableName: "quality_assessment_merged_pass")
    }

    changeSet(author: "-", id: "1697796529996-114") {
        dropTable(tableName: "quality_assessment_merged_pass")
    }

    changeSet(author: "-", id: "1697796529996-131") {
        dropColumn(columnName: "quality_assessment_merged_pass_id", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1697805756218-90") {
        createIndex(indexName: "abstract_quality_assessment_abstract_bam_file_idx", tableName: "abstract_quality_assessment") {
            column(name: "abstract_bam_file_id")
        }
    }
}
