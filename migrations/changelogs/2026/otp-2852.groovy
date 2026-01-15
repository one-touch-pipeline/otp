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
    changeSet(author: "strubelp", id: "otp-2852-1") {
        dropColumn(columnName: "configuration", tableName: "config_per_project_and_seq_type")
    }
    changeSet(author: "strubelp", id: "otp-2852-2") {
        renameTable(
                oldTableName: "roddy_snv_calling_instance_roddy_execution_directory_names",
                newTableName: "snv_calling_instance_roddy_execution_directory_names"
        )
    }
    changeSet(author: "strubelp", id: "otp-2852-3") {
        dropForeignKeyConstraint(
                baseTableName: "snv_calling_instance_roddy_execution_directory_names",
                constraintName: "roddy_snv_calling_instance_ro_roddy_snv_calling_instance_i_fkey"
        )
    }
    changeSet(author: "strubelp", id: "otp-2852-4") {
        renameColumn(
                tableName: "snv_calling_instance_roddy_execution_directory_names",
                oldColumnName: "roddy_snv_calling_instance_id",
                newColumnName: "snv_calling_instance_id"
        )
    }
    changeSet(author: "strubelp", id: "otp-2852-5") {
        addForeignKeyConstraint(
                constraintName: "snv_calling_instance_ro_snv_calling_instance_i_fkey",
                baseTableName: "snv_calling_instance_roddy_execution_directory_names",
                baseColumnNames: "snv_calling_instance_id",
                referencedTableName: "bam_file_pair_analysis",
                referencedColumnNames: "id"
        )
    }
    changeSet(author: "strubelp", id: "otp-2852-6") {
        sql("""
            UPDATE bam_file_pair_analysis
            SET class = 'de.dkfz.tbi.otp.dataprocessing.snvcalling.SnvCallingInstance'
            WHERE class = 'de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance'
        """)
    }
}
