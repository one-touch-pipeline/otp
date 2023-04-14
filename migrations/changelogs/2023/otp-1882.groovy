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

    changeSet(author: "-", id: "1681485356298-71") {
        addUniqueConstraint(columnNames: "abstract_merged_bam_file_id", constraintName: "UC_QUALITY_ASSESSMENT_MERGED_PASSABSTRACT_MERGED_BAM_FILE_ID_COL", tableName: "quality_assessment_merged_pass")
    }

    changeSet(author: "-", id: "1681485356298-77") {
        dropForeignKeyConstraint(baseTableName: "alignment_pass", constraintName: "alignment_pass_work_package_id_fkey")
    }

    changeSet(author: "-", id: "1681485356298-78") {
        dropForeignKeyConstraint(baseTableName: "merging_set_assignment", constraintName: "fk12b3e15448d3f8c4")
    }

    changeSet(author: "-", id: "1681485356298-79") {
        dropForeignKeyConstraint(baseTableName: "merging_set_assignment", constraintName: "fk12b3e154abebb43e")
    }

    changeSet(author: "-", id: "1681485356298-80") {
        dropForeignKeyConstraint(baseTableName: "abstract_quality_assessment", constraintName: "fk4525553f7c88f89")
    }

    changeSet(author: "-", id: "1681485356298-81") {
        dropForeignKeyConstraint(baseTableName: "merging_pass", constraintName: "fk80961b1b48d3f8c4")
    }

    changeSet(author: "-", id: "1681485356298-82") {
        dropForeignKeyConstraint(baseTableName: "abstract_bam_file", constraintName: "fk9ae4d28a2c1e7b10")
    }

    changeSet(author: "-", id: "1681485356298-83") {
        dropForeignKeyConstraint(baseTableName: "abstract_bam_file", constraintName: "fk9ae4d28a78a3e52c")
    }

    changeSet(author: "-", id: "1681485356298-84") {
        dropForeignKeyConstraint(baseTableName: "picard_mark_duplicates_metrics", constraintName: "fka1b0bd48131bef43")
    }

    changeSet(author: "-", id: "1681485356298-85") {
        dropForeignKeyConstraint(baseTableName: "merging_set", constraintName: "fkc67fc18cf07d60d")
    }

    changeSet(author: "-", id: "1681485356298-86") {
        dropForeignKeyConstraint(baseTableName: "processed_sai_file", constraintName: "fkce05e1315abdecb5")
    }

    changeSet(author: "-", id: "1681485356298-87") {
        dropForeignKeyConstraint(baseTableName: "processed_sai_file", constraintName: "fkce05e13178a3e52c")
    }

    changeSet(author: "-", id: "1681485356298-88") {
        dropForeignKeyConstraint(baseTableName: "alignment_pass", constraintName: "fkdf0830edf27d81e1")
    }

    changeSet(author: "-", id: "1681485356298-89") {
        dropForeignKeyConstraint(baseTableName: "quality_assessment_pass", constraintName: "fkef60082e2dead8cd")
    }

    changeSet(author: "-", id: "1681485356298-99") {
        dropUniqueConstraint(constraintName: "UK3c476766119bb42f2680ccfbe2b1", tableName: "quality_assessment_merged_pass")
    }

    changeSet(author: "-", id: "1681485356298-100") {
        dropUniqueConstraint(constraintName: "UK4fe362d3b753414fa9b1a80b6720", tableName: "merging_set")
    }

    changeSet(author: "-", id: "1681485356298-101") {
        dropUniqueConstraint(constraintName: "UK9f522e63c97479850e5d9b81286f", tableName: "alignment_pass")
    }

    changeSet(author: "-", id: "1681485356298-102") {
        dropUniqueConstraint(constraintName: "UKcb6a815f64f1f4ab82d06a967434", tableName: "quality_assessment_pass")
    }

    changeSet(author: "-", id: "1681485356298-103") {
        dropUniqueConstraint(constraintName: "UKef39ad3edbe91ab8ebbcfc0882ec", tableName: "merging_pass")
    }

    changeSet(author: "-", id: "1681485356298-109") {
        dropTable(tableName: "alignment_pass")
    }

    changeSet(author: "-", id: "1681485356298-110") {
        dropTable(tableName: "merging_pass")
    }

    changeSet(author: "-", id: "1681485356298-111") {
        dropTable(tableName: "merging_set")
    }

    changeSet(author: "-", id: "1681485356298-112") {
        dropTable(tableName: "merging_set_assignment")
    }

    changeSet(author: "-", id: "1681485356298-113") {
        dropTable(tableName: "picard_mark_duplicates_metrics")
    }

    changeSet(author: "-", id: "1681485356298-114") {
        dropTable(tableName: "processed_sai_file")
    }

    changeSet(author: "-", id: "1681485356298-115") {
        dropTable(tableName: "quality_assessment_pass")
    }

    changeSet(author: "-", id: "1681485356298-132") {
        dropColumn(columnName: "alignment_pass_id", tableName: "abstract_bam_file")
    }

    changeSet(author: "-", id: "1681485356298-133") {
        dropColumn(columnName: "chromosome_name", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-134") {
        dropColumn(columnName: "duplicater1", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-135") {
        dropColumn(columnName: "duplicater2", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-136") {
        dropColumn(columnName: "end_read_aberration", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-137") {
        dropColumn(columnName: "identifier", tableName: "quality_assessment_merged_pass")
    }

    changeSet(author: "-", id: "1681485356298-138") {
        dropColumn(columnName: "insert_sizerms", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-139") {
        dropColumn(columnName: "mapped_low_qualityr1", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-140") {
        dropColumn(columnName: "mapped_low_qualityr2", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-141") {
        dropColumn(columnName: "mapped_quality_longr1", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-142") {
        dropColumn(columnName: "mapped_quality_longr2", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-143") {
        dropColumn(columnName: "mapped_shortr1", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-144") {
        dropColumn(columnName: "mapped_shortr2", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-145") {
        dropColumn(columnName: "merging_pass_id", tableName: "abstract_bam_file")
    }

    changeSet(author: "-", id: "1681485356298-146") {
        dropColumn(columnName: "not_mappedr1", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-147") {
        dropColumn(columnName: "not_mappedr2", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-148") {
        dropColumn(columnName: "percent_incorrectpeorientation", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-149") {
        dropColumn(columnName: "percent_read_pairs_map_to_diff_chrom", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-150") {
        dropColumn(columnName: "proper_pair_strand_conflict", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-151") {
        dropColumn(columnName: "quality_assessment_pass_id", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-152") {
        dropColumn(columnName: "reference_agreement", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-153") {
        dropColumn(columnName: "reference_agreement_strand_conflict", tableName: "abstract_quality_assessment")
    }

    changeSet(author: "-", id: "1681485356298-154") {
        dropColumn(columnName: "status", tableName: "abstract_bam_file")
    }
}
