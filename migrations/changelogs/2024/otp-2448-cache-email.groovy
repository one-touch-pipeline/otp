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

    changeSet(author: "-", id: "1708616833193-89") {
        createTable(tableName: "mail") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "mailPK")
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

            column(name: "body", type: "TEXT") {
                constraints(nullable: "false")
            }

            column(name: "send_date_time", type: "TIMESTAMP WITH TIME ZONE")

            column(name: "state", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "subject", type: "VARCHAR(1000)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "-", id: "1708616833193-90") {
        createTable(tableName: "mail_bcc") {
            column(name: "mail_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "bcc_string", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "-", id: "1708616833193-91") {
        createTable(tableName: "mail_cc") {
            column(name: "mail_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "cc_string", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "-", id: "1708616833193-92") {
        createTable(tableName: "mail_to") {
            column(name: "mail_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "to_string", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "-", id: "1708616833193-93") {
        createIndex(indexName: "mail_state_date_created_idx", tableName: "mail") {
            column(name: "state")

            column(name: "date_created")
        }
    }

    changeSet(author: "-", id: "1708616833193-94") {
        addForeignKeyConstraint(baseColumnNames: "mail_id", baseTableName: "mail_to", constraintName: "FK64epad6w12obj2rwl0q10wx2g", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "mail", validate: "true")
    }

    changeSet(author: "-", id: "1708616833193-95") {
        addForeignKeyConstraint(baseColumnNames: "mail_id", baseTableName: "mail_bcc", constraintName: "FKa6gtufsixe82v5voyhccbujmi", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "mail", validate: "true")
    }

    changeSet(author: "-", id: "1708616833193-96") {
        addForeignKeyConstraint(baseColumnNames: "mail_id", baseTableName: "mail_cc", constraintName: "FKqvtommri700u1m4uva4nmw45w", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "mail", validate: "true")
    }

    changeSet(author: "-", id: "1708616833193-100") {
        createIndex(indexName: "mail_mail_to_idx", tableName: "mail_to") {
            column(name: "mail_id")
        }
    }

    changeSet(author: "-", id: "1708616833193-101") {
        createIndex(indexName: "mail_mail_cc_idx", tableName: "mail_cc") {
            column(name: "mail_id")
        }
    }

    changeSet(author: "-", id: "1708616833193-102") {
        createIndex(indexName: "mail_mail_bcc_idx", tableName: "mail_bcc") {
            column(name: "mail_id")
        }
    }
}
