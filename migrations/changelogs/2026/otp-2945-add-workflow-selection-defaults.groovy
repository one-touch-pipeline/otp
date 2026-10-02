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

    changeSet(author: "-", id: "otp-2945-100") {
        createTable(tableName: "workflow_default_group") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "workflow_default_groupPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "seq_type_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "-", id: "otp-2945-101") {
        createTable(tableName: "workflow_version_selector_default") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "workflow_version_selector_defaultPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "reference_genome_id", type: "BIGINT")

            column(name: "workflow_version_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "group_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "-", id: "otp-2945-102") {
        createTable(tableName: "workflow_version_selector_default_species_with_strain") {
            column(name: "workflow_version_selector_default_species_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "species_with_strain_id", type: "BIGINT")
        }
    }

    changeSet(author: "-", id: "otp-2945-103") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_WORKFLOW_DEFAULT_GROUPNAME_COL", tableName: "workflow_default_group")
    }

    changeSet(author: "-", id: "otp-2945-104") {
        createIndex(indexName: "workflow_default_group_seq_type_idx", tableName: "workflow_default_group") {
            column(name: "seq_type_id")
        }
    }

    changeSet(author: "-", id: "otp-2945-105") {
        createIndex(indexName: "workflow_version_selector_default_group_idx", tableName: "workflow_version_selector_default") {
            column(name: "group_id")
        }
    }

    changeSet(author: "-", id: "otp-2945-106") {
        createIndex(indexName: "workflow_version_selector_default_reference_genome_idx", tableName: "workflow_version_selector_default") {
            column(name: "reference_genome_id")
        }
    }

    changeSet(author: "-", id: "otp-2945-107") {
        createIndex(indexName: "workflow_version_selector_default_workflow_version_idx", tableName: "workflow_version_selector_default") {
            column(name: "workflow_version_id")
        }
    }

    changeSet(author: "-", id: "otp-2945-107a") {
        createIndex(indexName: "workflow_version_selector_default_species_idx", tableName: "workflow_version_selector_default_species_with_strain") {
            column(name: "workflow_version_selector_default_species_id")
        }
    }

    changeSet(author: "-", id: "otp-2945-107b") {
        createIndex(indexName: "workflow_version_selector_default_species_species_with_strain_idx", tableName: "workflow_version_selector_default_species_with_strain") {
            column(name: "species_with_strain_id")
        }
    }

    changeSet(author: "-", id: "otp-2945-108") {
        addForeignKeyConstraint(baseColumnNames: "species_with_strain_id", baseTableName: "workflow_version_selector_default_species_with_strain", constraintName: "FK3033mygoibsld4ixdr2w2ci8l", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_with_strain", validate: "true")
    }

    changeSet(author: "-", id: "otp-2945-109") {
        addForeignKeyConstraint(baseColumnNames: "workflow_version_id", baseTableName: "workflow_version_selector_default", constraintName: "FK6pur2w2mobdml3a3kifgjgjwu", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_version", validate: "true")
    }

    changeSet(author: "-", id: "otp-2945-110") {
        addForeignKeyConstraint(baseColumnNames: "workflow_version_selector_default_species_id", baseTableName: "workflow_version_selector_default_species_with_strain", constraintName: "FK8odp9pupacf4xholsesff85kf", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_version_selector_default", validate: "true")
    }

    changeSet(author: "-", id: "otp-2945-111") {
        addForeignKeyConstraint(baseColumnNames: "group_id", baseTableName: "workflow_version_selector_default", constraintName: "FK9sijy9b8r3p84qav6mfbv5usc", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_default_group", validate: "true")
    }

    changeSet(author: "-", id: "otp-2945-112") {
        addForeignKeyConstraint(baseColumnNames: "reference_genome_id", baseTableName: "workflow_version_selector_default", constraintName: "FKhe52meyeubp9q0hmupct196gq", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "reference_genome", validate: "true")
    }

    changeSet(author: "-", id: "otp-2945-113") {
        addForeignKeyConstraint(baseColumnNames: "seq_type_id", baseTableName: "workflow_default_group", constraintName: "FKk7g3khpj6is6b9qj3jdn1jq5", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_type", validate: "true")
    }
}
