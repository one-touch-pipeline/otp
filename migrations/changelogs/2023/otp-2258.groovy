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

    changeSet(author: "Julian Rausch", id: "1702907358337-88") {
        createTable(tableName: "workflow_api_version") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "workflow_api_versionPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "workflow_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "bean_name", type: "VARCHAR(255)")

            column(name: "identifier", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    // Create a workflow api version with identifier 1 for every existing workflow
    changeSet(author: "Julian Rausch", id: "1702907358337-89") {
        sql("INSERT INTO workflow_api_version (id, date_created, last_updated, version, workflow_id, identifier) SELECT NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, id, 1 FROM workflow")
    }

    changeSet(author: "Julian Rausch", id: "1702907358337-90") {
        addColumn(tableName: "workflow_version") {
            column(name: "api_version_id", type: "int8")
        }
    }

    // Replace workflow with create workflow api version in workflow version
   changeSet(author: "Julian Rausch", id: "1702907358337-90a") {
       sql("UPDATE workflow_version SET api_version_id = (SELECT wav.id FROM workflow_api_version wav WHERE wav.workflow_id = workflow_version.workflow_id)")
   }

    changeSet(author: "Julian Rausch", id: "1702907358337-94") {
        dropForeignKeyConstraint(baseTableName: "workflow_version", constraintName: "FKgjhy6w5q8buydj1hboch9dxl5")
    }

    changeSet(author: "Julian Rausch", id: "1702907358337-109") {
        dropUniqueConstraint(constraintName: "UKcb8bd959752a92ddf76fef03121d", tableName: "workflow_version")
    }

    changeSet(author: "Julian Rausch", id: "1702907358337-131") {
        dropColumn(columnName: "workflow_id", tableName: "workflow_version")
    }

    changeSet(author: "Julian Rausch", id: "1702907358337-92") {
        addForeignKeyConstraint(baseColumnNames: "api_version_id", baseTableName: "workflow_version", constraintName: "FK9uxpqgporhkj9itq4mcd4qgqe", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName:
                "workflow_api_version", validate: "true")
    }

    changeSet(author: "Julian Rausch", id: "1702907358337-93") {
        addForeignKeyConstraint(baseColumnNames: "workflow_id", baseTableName: "workflow_api_version", constraintName: "FKdotpidph6jh13medagp4ac73o", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName:
                "workflow", validate: "true")
    }

    changeSet(author: "Julian Rausch", id: "1702911649714-90") {
        addUniqueConstraint(columnNames: "workflow_id, identifier", constraintName: "UKf4cfb45fc9bba0bbd4530a385e68", tableName: "workflow_api_version")
    }

    changeSet(author: "Julian Rausch", id: "1702911649714-2") {
        addNotNullConstraint(columnDataType: "bigint", columnName: "api_version_id", tableName: "workflow_version", validate: "true")
    }

    changeSet(author: "Julian Rausch", id: "1702971533169-90") {
        addUniqueConstraint(columnNames: "workflow_version, api_version_id", constraintName: "UK4b9a4bf6561a94d08c220c387977", tableName: "workflow_version")
    }

    changeSet(author: "Julian Rausch", id: "1702971533169-91") {
        createIndex(indexName: "workflow_version_workflow_api_version_idx", tableName: "workflow_version") {
            column(name: "api_version_id")
        }
    }
    changeSet(author: "Julian Rausch", id: "1702996061174-89") {
        createIndex(indexName: "workflow_api_version_workflow_idx", tableName: "workflow_api_version") {
            column(name: "workflow_id")
        }
    }
}
