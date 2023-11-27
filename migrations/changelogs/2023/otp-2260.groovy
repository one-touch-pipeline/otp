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

    changeSet(author: "Julian Rausch", id: "1698409704777-90") {
        createTable(tableName: "workflow_version_reference_genome") {
            column(name: "workflow_version_allowed_reference_genomes_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "reference_genome_id", type: "BIGINT")
        }
    }

    changeSet(author: "Julian Rausch", id: "1698409704777-89") {
        createIndex(indexName: "workflow_version_allowed_reference_genomes_idx", tableName: "workflow_version_reference_genome") {
            column(name: "workflow_version_allowed_reference_genomes_id")
        }
        createIndex(indexName: "workflow_version_reference_genome_idx", tableName: "workflow_version_reference_genome") {
            column(name: "reference_genome_id")
        }
    }

    changeSet(author: "Julian Rausch", id: "1698409704777-91") {
        createTable(tableName: "workflow_version_seq_type") {
            column(name: "workflow_version_supported_seq_types_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "seq_type_id", type: "BIGINT")
        }
    }


    changeSet(author: "Julian Rausch", id: "1698409704777-88") {
        createIndex(indexName: "workflow_version_supported_seq_types_idx", tableName: "workflow_version_seq_type") {
            column(name: "workflow_version_supported_seq_types_id")
        }
        createIndex(indexName: "workflow_version_seq_type_idx", tableName: "workflow_version_seq_type") {
            column(name: "seq_type_id")
        }
    }

    changeSet(author: "Julian Rausch", id: "1698409704777-92") {
        renameColumn(tableName: "workflow_reference_genome", oldColumnName: "workflow_allowed_reference_genomes_id", newColumnName: "workflow_default_reference_genomes_for_workflow_versions_id") {}
    }

    changeSet(author: "Julian Rausch", id: "1698409704777-93") {
        renameColumn(tableName: "workflow_seq_type", oldColumnName: "workflow_supported_seq_types_id", newColumnName: "workflow_default_seq_types_for_workflow_versions_id") {}
    }

    changeSet(author: "Julian Rausch", id: "1698409704777-94") {
        addForeignKeyConstraint(baseColumnNames: "workflow_version_supported_seq_types_id", baseTableName: "workflow_version_seq_type", constraintName: "FK369mkhu9vuqmn44dq9uj90hmr", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_version", validate: "true")
    }

    changeSet(author: "Julian Rausch", id: "1698409704777-97") {
        addForeignKeyConstraint(baseColumnNames: "workflow_version_allowed_reference_genomes_id", baseTableName: "workflow_version_reference_genome", constraintName: "FKhj6g1f778kq9u9b42ktss0ric", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_version", validate: "true")
    }

    changeSet(author: "Julian Rausch", id: "1698409704777-98") {
        addForeignKeyConstraint(baseColumnNames: "reference_genome_id", baseTableName: "workflow_version_reference_genome", constraintName: "FKok81i4ohoftaxtbmoqcn7941q", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "reference_genome", validate: "true")
    }

    changeSet(author: "Julian Rausch", id: "1698409704777-99") {
        addForeignKeyConstraint(baseColumnNames: "seq_type_id", baseTableName: "workflow_version_seq_type", constraintName: "FKrjuhmcbgvt2rtwwk0om6pv75s", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_type", validate: "true")
    }
}
