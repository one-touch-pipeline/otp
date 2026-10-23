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
    changeSet(author: "pieper", id: "otp-2649-1") {
        createTable(tableName: "data_swap_info") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "data_swap_infoPK")
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
            column(name: "seq_track_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "swap_info", type: "TEXT") {
                constraints(nullable: "false")
            }
            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "pieper", id: "otp-2649-2") {
        addForeignKeyConstraint(
                constraintName: "data_swap_info_seq_track_fkey",
                baseTableName: "data_swap_info",
                baseColumnNames: "seq_track_id",
                referencedTableName: "seq_track",
                referencedColumnNames: "id",
        )

        addForeignKeyConstraint(
                constraintName: "data_swap_info_user_fkey",
                baseTableName: "data_swap_info",
                baseColumnNames: "user_id",
                referencedTableName: "users",
                referencedColumnNames: "id",
        )
    }

    changeSet(author: "pieper", id: "otp-2649-3") {
        createIndex(indexName: "data_swap_info_seq_track_idx", tableName: "data_swap_info") {
            column(name: "seq_track_id")
        }

        createIndex(indexName: "data_swap_info_user_idx", tableName: "data_swap_info") {
            column(name: "user_id")
        }
    }

    changeSet(author: "pieper", id: "otp-2649-4") {
        createTable(tableName: "individual_ticket") {
            column(name: "individual_data_swap_tickets_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "ticket_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "pieper", id: "otp-2649-5") {
        addForeignKeyConstraint(
                constraintName: "individual_ticket_individual_fkey",
                baseTableName: "individual_ticket",
                baseColumnNames: "individual_data_swap_tickets_id",
                referencedTableName: "individual",
                referencedColumnNames: "id",
        )

        addForeignKeyConstraint(
                constraintName: "individual_ticket_ticket_fkey",
                baseTableName: "individual_ticket",
                baseColumnNames: "ticket_id",
                referencedTableName: "ticket",
                referencedColumnNames: "id",
        )
    }

    changeSet(author: "pieper", id: "otp-2649-6") {
        createIndex(indexName: "individual_ticket_individual_idx", tableName: "individual_ticket") {
            column(name: "individual_data_swap_tickets_id")
        }

        createIndex(indexName: "individual_ticket_ticket_idx", tableName: "individual_ticket") {
            column(name: "ticket_id")
        }
    }
}

