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
    changeSet(author: "", id: "1636556284595-1") {
        createTable(tableName: "selected_project_seq_type_workflow_version") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "object_version", type: "INT") {
                constraints(nullable: "false")
            }
            column(name: "workflow_version_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }
            column(name: "previous_id", type: "BIGINT")
            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }
            column(name: "deprecation_date", type: "date")
            column(name: "seq_type_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "project_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }
    changeSet(author: "", id: "1636556284595-2") {
        addColumn(tableName: "reference_genome_selector") {
            column(name: "project_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }
    changeSet(author: "", id: "1636556284595-3") {
        addColumn(tableName: "reference_genome_selector") {
            column(name: "seq_type_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }
    changeSet(author: "", id: "1636556284595-4") {
        addColumn(tableName: "reference_genome_selector") {
            column(name: "species_common_name_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }
    changeSet(author: "", id: "1636556284595-5") {
        addColumn(tableName: "reference_genome_selector") {
            column(name: "workflow_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }
    changeSet(author: "", id: "1636556284595-6") {
        addPrimaryKey(columnNames: "id", constraintName: "selected_project_seq_type_workflow_versionPK", tableName: "selected_project_seq_type_workflow_version")
    }
    changeSet(author: "", id: "1636556284595-9") {
        addForeignKeyConstraint(baseColumnNames: "seq_type_id", baseTableName: "selected_project_seq_type_workflow_version", constraintName: "FK1u7ox5ktwedv5uvcgt9j8o0bl", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_type")
    }
    changeSet(author: "", id: "1636556284595-10") {
        addForeignKeyConstraint(baseColumnNames: "species_common_name_id", baseTableName: "reference_genome_selector", constraintName: "FK6yqetv51kleoaav4fmiguke4u", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_common_name")
    }
    changeSet(author: "", id: "1636556284595-11") {
        addForeignKeyConstraint(baseColumnNames: "project_id", baseTableName: "selected_project_seq_type_workflow_version", constraintName: "FK6ywfo4l6v9tf93oracynlim2o", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project")
    }
    changeSet(author: "", id: "1636556284595-12") {
        addForeignKeyConstraint(baseColumnNames: "previous_id", baseTableName: "selected_project_seq_type_workflow_version", constraintName: "FKin6n213c2alv2x5po85y2ds5m", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "selected_project_seq_type_workflow_version")
    }
    changeSet(author: "", id: "1636556284595-13") {
        addForeignKeyConstraint(baseColumnNames: "project_id", baseTableName: "reference_genome_selector", constraintName: "FKkj2sds6v3e892cqoltmi5tmas", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project")
    }
    changeSet(author: "", id: "1636556284595-14") {
        addForeignKeyConstraint(baseColumnNames: "workflow_id", baseTableName: "reference_genome_selector", constraintName: "FKn5vw01rtunramasshavae0has", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow")
    }
    changeSet(author: "", id: "1636556284595-15") {
        addForeignKeyConstraint(baseColumnNames: "seq_type_id", baseTableName: "reference_genome_selector", constraintName: "FKqukg045oa53b5m0qj54itn0yp", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_type")
    }
    changeSet(author: "", id: "1636556284595-16") {
        addForeignKeyConstraint(baseColumnNames: "workflow_version_id", baseTableName: "selected_project_seq_type_workflow_version", constraintName: "FKr0vxlruj2xsowvj2hr00dx97d", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_version")
    }
    changeSet(author: "", id: "1636556284595-17") {
        dropForeignKeyConstraint(baseTableName: "active_project_workflow", constraintName: "FK95jki3fxnn1vuhd8aw1nwx611")
    }
    changeSet(author: "", id: "1636556284595-18") {
        dropForeignKeyConstraint(baseTableName: "active_project_workflow", constraintName: "FKhmo2tg9yp076rdr4jw657stin")
    }
    changeSet(author: "", id: "1636556284595-19") {
        dropForeignKeyConstraint(baseTableName: "reference_genome_selector_active_project_workflow", constraintName: "FKhww4kaejsak9wqibahvuofcwl")
    }
    changeSet(author: "", id: "1636556284595-20") {
        dropForeignKeyConstraint(baseTableName: "active_project_workflow", constraintName: "FKhxyrw6ba20n0orcxjnr6xr9yh")
    }
    changeSet(author: "", id: "1636556284595-25") {
        dropForeignKeyConstraint(baseTableName: "reference_genome_selector_active_project_workflow", constraintName: "FKnprkx4jcx82515eq7p6xye2d4")
    }
    changeSet(author: "", id: "1636556284595-26") {
        dropForeignKeyConstraint(baseTableName: "active_project_workflow", constraintName: "FKoiobjb5mn07vacpvdvtsk03xr")
    }
    changeSet(author: "", id: "1636556284595-41") {
        dropTable(tableName: "active_project_workflow")
    }
    changeSet(author: "", id: "1636556284595-42") {
        dropTable(tableName: "reference_genome_selector_active_project_workflow")
    }
    changeSet(author: "", id: "1636556284595-60") {
        dropColumn(columnName: "specific_reference_genome", tableName: "reference_genome_selector")
    }
}