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

    changeSet(author: "borufka", id: "otp-3133-1") {
        createTable(tableName: "data_type") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "data_typePK")
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

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka", id: "otp-3133-2") {
        createTable(tableName: "data_type_key") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "data_type_keyPK")
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

            column(name: "data_type_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "key_type", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka", id: "otp-3133-3") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_DATA_TYPENAME_COL", tableName: "data_type")
    }

    changeSet(author: "borufka", id: "otp-3133-4") {
        addUniqueConstraint(columnNames: "data_type_id, name", constraintName: "UKca004caf551f2fd54cae62efe1ab", tableName: "data_type_key")
    }

    changeSet(author: "borufka", id: "otp-3133-5") {
        createIndex(indexName: "data_type_key_data_type_idx", tableName: "data_type_key") {
            column(name: "data_type_id")
        }
    }

    changeSet(author: "borufka", id: "otp-3133-6") {
        addForeignKeyConstraint(baseColumnNames: "data_type_id", baseTableName: "data_type_key", constraintName: "FKeomxx7e1u4f3uwjta30pm9s2e", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "data_type", validate: "true")
    }

    changeSet(author: "borufka", id: "otp-3133-7") {
        createTable(tableName: "data_type_version") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "data_type_versionPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "raw_json_schema", type: "TEXT") {
                constraints(nullable: "false")
            }

            column(name: "data_type_version", type: "INTEGER") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "data_type_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka", id: "otp-3133-8") {
        createTable(tableName: "data_type_version_data_type_key") {
            column(name: "data_type_version_keys_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "key_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka", id: "otp-3133-9") {
        addUniqueConstraint(columnNames: "data_type_id, data_type_version", constraintName: "UK285b1539a36dfaa3d401dd8bb592", tableName: "data_type_version")
    }

    changeSet(author: "borufka", id: "otp-3133-10") {
        createIndex(indexName: "data_type_version_data_type_idx", tableName: "data_type_version") {
            column(name: "data_type_id")
        }
    }

    changeSet(author: "borufka", id: "otp-3133-11") {
        addForeignKeyConstraint(baseColumnNames: "data_type_version_keys_id", baseTableName: "data_type_version_data_type_key", constraintName: "FK3c6n1h0bdadsh0we53lbmrk3f", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "data_type_version", validate: "true")
    }

    changeSet(author: "borufka", id: "otp-3133-12") {
        addForeignKeyConstraint(baseColumnNames: "data_type_id", baseTableName: "data_type_version", constraintName: "FKoux789yfgsyuxl77ruv7xl70s", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "data_type", validate: "true")
    }

    changeSet(author: "borufka", id: "otp-3133-13") {
        addForeignKeyConstraint(baseColumnNames: "key_id", baseTableName: "data_type_version_data_type_key", constraintName: "FKrwc3nsrdnpscssu1ytqroji2e", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "data_type_key", validate: "true")
    }

    changeSet(author: "borufka", id: "otp-3133-14") {
        createIndex(indexName: "data_type_version_data_type_key_data_type_version_idx", tableName: "data_type_version_data_type_key") {
            column(name: "data_type_version_keys_id")
        }
    }

    changeSet(author: "borufka", id: "otp-3133-15") {
        createIndex(indexName: "data_type_version_data_type_key_key_idx", tableName: "data_type_version_data_type_key") {
            column(name: "key_id")
        }
    }

    changeSet(author: "borufka", id: "otp-3133-16") {
        createTable(tableName: "data_set") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "data_setPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "data_type_version_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "raw_json_data", type: "TEXT") {
                constraints(nullable: "false")
            }

            column(name: "project_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka", id: "otp-3133-17") {
        createIndex(indexName: "data_set_data_type_version_idx", tableName: "data_set") {
            column(name: "data_type_version_id")
        }
    }

    changeSet(author: "borufka", id: "otp-3133-18") {
        createIndex(indexName: "data_set_project_idx", tableName: "data_set") {
            column(name: "project_id")
        }
    }

    changeSet(author: "borufka", id: "otp-3133-19") {
        addForeignKeyConstraint(baseColumnNames: "project_id", baseTableName: "data_set", constraintName: "FK6dm1leyf64g52hj50328lo5qt", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project", validate: "true")
    }

    changeSet(author: "borufka", id: "otp-3133-20") {
        addForeignKeyConstraint(baseColumnNames: "data_type_version_id", baseTableName: "data_set", constraintName: "FKkgpte9wakoexgv9oq3ndocugg", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "data_type_version", validate: "true")
    }

    changeSet(author: "borufka", id: "otp-3133-21") {
        createTable(tableName: "register_data_set") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "register_data_setPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "data_type_version_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "state", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "raw_json_data", type: "TEXT") {
                constraints(nullable: "false")
            }

            column(name: "priority_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "ticket_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka", id: "otp-3133-22") {
        createTable(tableName: "register_data_set_project") {
            column(name: "register_data_set_projects_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "project_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka", id: "otp-3133-23") {
        createIndex(indexName: "register_data_set_data_type_version_idx", tableName: "register_data_set") {
            column(name: "data_type_version_id")
        }
    }

    changeSet(author: "borufka", id: "otp-3133-24") {
        createIndex(indexName: "register_data_set_priority_idx", tableName: "register_data_set") {
            column(name: "priority_id")
        }
    }

    changeSet(author: "borufka", id: "otp-3133-25") {
        createIndex(indexName: "register_data_set_state_idx", tableName: "register_data_set") {
            column(name: "state")
        }
    }

    changeSet(author: "borufka", id: "otp-3133-26") {
        createIndex(indexName: "register_data_set_ticket_idx", tableName: "register_data_set") {
            column(name: "ticket_id")
        }
    }

    changeSet(author: "borufka", id: "otp-3133-27") {
        addForeignKeyConstraint(baseColumnNames: "data_type_version_id", baseTableName: "register_data_set", constraintName: "FK4aol9vfn4kpig6bkd1h2dm5as", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "data_type_version", validate: "true")
    }

    changeSet(author: "borufka", id: "otp-3133-28") {
        addForeignKeyConstraint(baseColumnNames: "priority_id", baseTableName: "register_data_set", constraintName: "FKeoicjal6abm1j1tnxboi8q37p", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "processing_priority", validate: "true")
    }

    changeSet(author: "borufka", id: "otp-3133-29") {
        addForeignKeyConstraint(baseColumnNames: "project_id", baseTableName: "register_data_set_project", constraintName: "FKfhc3n3t9glcrvjwe5knvdl0oo", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project", validate: "true")
    }

    changeSet(author: "borufka", id: "otp-3133-30") {
        addForeignKeyConstraint(baseColumnNames: "register_data_set_projects_id", baseTableName: "register_data_set_project", constraintName: "FKh12jmv6j5ov8cipf5kl7sel5r", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "register_data_set", validate: "true")
    }

    changeSet(author: "borufka", id: "otp-3133-31") {
        addForeignKeyConstraint(baseColumnNames: "ticket_id", baseTableName: "register_data_set", constraintName: "FKmytmtx9btbc426dyh0ynrynv6", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "ticket", validate: "true")
    }

    changeSet(author: "borufka", id: "otp-3133-32") {
        createIndex(indexName: "register_data_set_project_register_data_set_idx", tableName: "register_data_set_project") {
            column(name: "register_data_set_projects_id")
        }
    }

    changeSet(author: "borufka", id: "otp-3133-33") {
        createIndex(indexName: "register_data_set_project_project_idx", tableName: "register_data_set_project") {
            column(name: "project_id")
        }
    }

    changeSet(author: "borufka", id: "otp-3133-34") {
        addColumn(tableName: "data_set") {
            column(name: "ticket_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka", id: "otp-3133-35") {
        createIndex(indexName: "data_set_ticket_idx", tableName: "data_set") {
            column(name: "ticket_id")
        }
    }

    changeSet(author: "borufka", id: "otp-3133-36") {
        addForeignKeyConstraint(baseColumnNames: "ticket_id", baseTableName: "data_set", constraintName: "FK7s8iqop4wwscl2vf3m84bgmhi", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "ticket", validate: "true")
    }
}
