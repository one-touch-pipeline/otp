/*
 * Copyright 2011-2020 The OTP authors
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
    changeSet(author: "borufka (generated)", id: "1591616493531-1") {
        createTable(tableName: "abstract_field_definition") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "class", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "cardinality_type", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "description_config", type: "TEXT") {
                constraints(nullable: "false")
            }

            column(name: "description_request", type: "TEXT") {
                constraints(nullable: "false")
            }

            column(name: "field_use_for_sequencing_projects", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "field_use_for_data_management_projects", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
            column(name: "source_of_data", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "project_display_on_config_page", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "sort_number", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "change_only_by_operator", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "used_externally", type: "BOOLEAN") {
                constraints(nullable: "false")
            }


            column(name: "legacy", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "default_text_value", type: "TEXT")

            column(name: "default_integer_value", type: "INT")

            column(name: "default_decimal_number_value", type: "FLOAT8")

            column(name: "default_flag_value", type: "BOOLEAN")

            column(name: "default_date_value", type: "date")

            column(name: "regular_expression", type: "TEXT")

            column(name: "type_validator", type: "VARCHAR(255)")

            column(name: "domain_class_name", type: "VARCHAR(255)")

            column(name: "default_domain_reference_id", type: "BIGINT")

            column(name: "allow_custom_value", type: "BOOLEAN")

        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-2") {
        createTable(tableName: "abstract_field_value") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "class", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "definition_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "text_value", type: "TEXT")

            column(name: "integer_value", type: "INT")

            column(name: "decimal_number_value", type: "FLOAT8")

            column(name: "date_value", type: "date")

            column(name: "flag_value", type: "BOOLEAN")

            column(name: "domain_id", type: "BIGINT")

            column(name: "cached_text_representation_of_domain", type: "VARCHAR(255)")

            column(name: "custom_value", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-3") {
        createTable(tableName: "date_field_definition_allowed_date_values") {
            column(name: "date_field_definition_id", type: "BIGINT")

            column(name: "allowed_date_values_local_date", type: "date")

            column(name: "allowed_date_values_idx", type: "INT")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-4") {
        createTable(tableName: "decimal_number_field_definition_allowed_decimal_number_values") {
            column(name: "decimal_number_field_definition_id", type: "BIGINT")

            column(name: "allowed_decimal_number_values_double", type: "FLOAT8")

            column(name: "allowed_decimal_number_values_idx", type: "INT")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-5") {
        createTable(tableName: "integer_field_definition_allowed_integer_values") {
            column(name: "integer_field_definition_id", type: "BIGINT")

            column(name: "allowed_integer_values_integer", type: "INT")

            column(name: "allowed_integer_values_idx", type: "INT")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-6") {
        createTable(tableName: "project_abstract_field_value") {
            column(name: "project_project_fields_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "abstract_field_value_id", type: "BIGINT")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-7") {
        createTable(tableName: "project_request_abstract_field_value") {
            column(name: "project_request_project_fields_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "abstract_field_value_id", type: "BIGINT")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-8") {
        createTable(tableName: "set_value_field_abstract_single_field_value") {
            column(name: "set_value_field_values_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "abstract_single_field_value_id", type: "BIGINT")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-9") {
        createTable(tableName: "text_field_definition_allowed_text_values") {
            column(name: "text_field_definition_id", type: "BIGINT")

            column(name: "allowed_text_values_string", type: "VARCHAR(255)")

            column(name: "allowed_text_values_idx", type: "INT")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-10") {
        addPrimaryKey(columnNames: "id", constraintName: "abstract_field_definitionPK", tableName: "abstract_field_definition")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-11") {
        addPrimaryKey(columnNames: "id", constraintName: "abstract_field_valuePK", tableName: "abstract_field_value")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-14") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_ABSTRACT_FIELD_DEFINITIONNAME_COL", tableName: "abstract_field_definition")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-15") {
        addForeignKeyConstraint(baseColumnNames: "abstract_single_field_value_id", baseTableName: "set_value_field_abstract_single_field_value",
                constraintName: "FK26egcjsvpgxe5ldrkh299k3oa", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "abstract_field_value")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-16") {
        addForeignKeyConstraint(baseColumnNames: "abstract_field_value_id", baseTableName: "project_abstract_field_value",
                constraintName: "FK8s9rw373nrs3lc0ps8btr92im", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "abstract_field_value")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-17") {
        addForeignKeyConstraint(baseColumnNames: "abstract_field_value_id", baseTableName: "project_request_abstract_field_value",
                constraintName: "FK8wqg2n8t1ua7xq22i3idobwxw", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "abstract_field_value")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-18") {
        addForeignKeyConstraint(baseColumnNames: "project_request_project_fields_id", baseTableName: "project_request_abstract_field_value",
                constraintName: "FK9vqbddv6ksvwaqr8q36becdwh", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-19") {
        addForeignKeyConstraint(baseColumnNames: "set_value_field_values_id", baseTableName: "set_value_field_abstract_single_field_value",
                constraintName: "FKh7fepy5j2qfhkou1olx9b8ggv", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "abstract_field_value")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-20") {
        addForeignKeyConstraint(baseColumnNames: "definition_id", baseTableName: "abstract_field_value",
                constraintName: "FKiffshu69us1d969pa1l9o0xcy", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "abstract_field_definition")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-21") {
        addForeignKeyConstraint(baseColumnNames: "project_project_fields_id", baseTableName: "project_abstract_field_value",
                constraintName: "FKisqi717i5p94j02phpecgkd5m",  deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-22") {
        addForeignKeyConstraint(baseColumnNames: "date_field_definition_id", baseTableName: "date_field_definition_allowed_date_values",
                constraintName: "FKisqi717i5p94j02phpecgkd5m2", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "abstract_field_definition")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-23") {
        addForeignKeyConstraint(baseColumnNames: "decimal_number_field_definition_id", baseTableName: "decimal_number_field_definition_allowed_decimal_number_values",
                constraintName: "FKisqi717i5p94j02phpecgkd5m3", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "abstract_field_definition")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-24") {
        addForeignKeyConstraint(baseColumnNames: "integer_field_definition_id", baseTableName: "integer_field_definition_allowed_integer_values",
                constraintName: "FKisqi717i5p94j02phpecgkd5m4", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "abstract_field_definition")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-25") {
        addForeignKeyConstraint(baseColumnNames: "text_field_definition_id", baseTableName: "text_field_definition_allowed_text_values",
                constraintName: "FKisqi717i5p94j02phpecgkd5m5", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "abstract_field_definition")
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-26") {
        createIndex(indexName: "abstract_field_value_definition_idx", tableName: "abstract_field_value") {
            column(name: "definition_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-27") {
        createIndex(indexName: "date_field_definition_allowed_date_values_date_field_definition_idx", tableName: "date_field_definition_allowed_date_values") {
            column(name: "date_field_definition_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-28") {
        createIndex(indexName: "decimal_number_field_definition_allowed_decimal_number_values_decimal_number_field_definition_id", tableName: "decimal_number_field_definition_allowed_decimal_number_values") {
            column(name: "decimal_number_field_definition_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-29") {
        createIndex(indexName: "integer_field_definition_allowed_integer_values_integer_field_definition_idx", tableName: "integer_field_definition_allowed_integer_values") {
            column(name: "integer_field_definition_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-30") {
        createIndex(indexName: "text_field_definition_allowed_text_values_text_field_definition_idx", tableName: "text_field_definition_allowed_text_values") {
            column(name: "text_field_definition_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-31") {
        createIndex(indexName: "set_value_field_abstract_single_field_value_abstract_single_field_value_idx", tableName: "set_value_field_abstract_single_field_value") {
            column(name: "abstract_single_field_value_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-32") {
        createIndex(indexName: "set_value_field_abstract_single_field_value_set_value_field_values_idx", tableName: "set_value_field_abstract_single_field_value") {
            column(name: "set_value_field_values_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-33") {
        createIndex(indexName: "project_request_abstract_field_value_project_request_project_fields_idx", tableName: "project_request_abstract_field_value") {
            column(name: "project_request_project_fields_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-34") {
        createIndex(indexName: "project_request_abstract_field_value_abstract_field_value_idx", tableName: "project_request_abstract_field_value") {
            column(name: "abstract_field_value_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-35") {
        createIndex(indexName: "project_abstract_field_value_project_project_fields_idx", tableName: "project_abstract_field_value") {
            column(name: "project_project_fields_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1591616493531-36") {
        createIndex(indexName: "project_abstract_field_value_abstract_field_value_idx", tableName: "project_abstract_field_value") {
            column(name: "abstract_field_value_id")
        }
    }

    changeSet(author: "otp (generated)", id: "1591707826049-3") {
        addForeignKeyConstraint(baseColumnNames: "comment_id", baseTableName: "individual", constraintName: "FK69vjbfxq2vbvgbelt1co6vgu", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "comment")
    }
}
