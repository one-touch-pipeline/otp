databaseChangeLog = {

    changeSet(author: "borufka (generated)", id: "1557818756576-1") {
        addColumn(tableName: "ega_library_selection_library_preparation_kit") {
            column(name: "ega_library_selection_library_preparation_kits_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-2") {
        addColumn(tableName: "ega_library_source_seq_type") {
            column(name: "ega_library_source_seq_types_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-3") {
        addColumn(tableName: "ega_library_strategy_seq_type") {
            column(name: "ega_library_strategy_seq_types_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-4") {
        addColumn(tableName: "ega_platform_model_seq_platform_model_label") {
            column(name: "ega_platform_model_seq_platform_model_labels_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-7") {
        addForeignKeyConstraint(baseColumnNames: "ega_platform_model_seq_platform_model_labels_id", baseTableName: "ega_platform_model_seq_platform_model_label", constraintName: "FK7fwld2di8xuqrbwrgn3491mhm", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "ega_platform_model")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-8") {
        addForeignKeyConstraint(baseColumnNames: "ega_library_source_seq_types_id", baseTableName: "ega_library_source_seq_type", constraintName: "FKc2x64x7h24c2m8dgo1arpyq49", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "ega_library_source")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-9") {
        addForeignKeyConstraint(baseColumnNames: "ega_library_strategy_seq_types_id", baseTableName: "ega_library_strategy_seq_type", constraintName: "FKohsccqo6onn7g3mj0lrwtpmyr", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "ega_library_strategy")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-10") {
        addForeignKeyConstraint(baseColumnNames: "ega_library_selection_library_preparation_kits_id", baseTableName: "ega_library_selection_library_preparation_kit", constraintName: "FKqnwi08tdiqy0vqnb8sk9s25d9", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "ega_library_selection")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-12") {
        dropForeignKeyConstraint(baseTableName: "ega_library_selection_library_preparation_kit", constraintName: "ega_library_selection_library_pre_ega_library_selection_id_fkey")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-13") {
        dropForeignKeyConstraint(baseTableName: "ega_library_source_seq_type", constraintName: "ega_library_source_seq_type_ega_library_source_id_fkey")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-14") {
        dropForeignKeyConstraint(baseTableName: "ega_library_strategy_seq_type", constraintName: "ega_library_strategy_seq_type_ega_library_strategy_id_fkey")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-15") {
        dropForeignKeyConstraint(baseTableName: "ega_platform_model_seq_platform_model_label", constraintName: "ega_platform_model_seq_platform_mode_ega_platform_model_id_fkey")
    }

    changeSet(author: "borufka (generated)", id: "1557819282305-28") {
        dropPrimaryKey(constraintName: "ega_library_selection_library_preparation_kit_pkey", tableName: "ega_library_selection_library_preparation_kit")
    }

    changeSet(author: "borufka (generated)", id: "1557819282305-29") {
        dropPrimaryKey(constraintName: "ega_library_source_seq_type_pkey", tableName: "ega_library_source_seq_type")
    }

    changeSet(author: "borufka (generated)", id: "1557819282305-30") {
        dropPrimaryKey(constraintName: "ega_library_strategy_seq_type_pkey", tableName: "ega_library_strategy_seq_type")
    }

    changeSet(author: "borufka (generated)", id: "1557819282305-31") {
        dropPrimaryKey(constraintName: "ega_platform_model_seq_platform_model_label_pkey", tableName: "ega_platform_model_seq_platform_model_label")
    }

    changeSet(author: "borufka (generated)", id: "1557819282305-44") {
        dropColumn(columnName: "ega_library_selection_id", tableName: "ega_library_selection_library_preparation_kit")
    }

    changeSet(author: "borufka (generated)", id: "1557819282305-45") {
        dropColumn(columnName: "ega_library_source_id", tableName: "ega_library_source_seq_type")
    }

    changeSet(author: "borufka (generated)", id: "1557819282305-46") {
        dropColumn(columnName: "ega_library_strategy_id", tableName: "ega_library_strategy_seq_type")
    }

    changeSet(author: "borufka (generated)", id: "1557819282305-47") {
        dropColumn(columnName: "ega_platform_model_id", tableName: "ega_platform_model_seq_platform_model_label")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-63") {
        dropNotNullConstraint(columnDataType: "bigint", columnName: "library_preparation_kit_id", tableName: "ega_library_selection_library_preparation_kit")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-78") {
        dropNotNullConstraint(columnDataType: "bigint", columnName: "seq_platform_model_label_id", tableName: "ega_platform_model_seq_platform_model_label")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-83") {
        dropNotNullConstraint(columnDataType: "bigint", columnName: "seq_type_id", tableName: "ega_library_source_seq_type")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-84") {
        dropNotNullConstraint(columnDataType: "bigint", columnName: "seq_type_id", tableName: "ega_library_strategy_seq_type")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-85") {
        addPrimaryKey(tableName: "ega_library_selection_library_preparation_kit", columnNames: "ega_library_selection_library_preparation_kits_id,library_preparation_kit_id")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-86") {
        addPrimaryKey(tableName: "ega_library_source_seq_type", columnNames: "ega_library_source_seq_types_id,seq_type_id")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-87") {
        addPrimaryKey(tableName: "ega_library_strategy_seq_type", columnNames: "ega_library_strategy_seq_types_id,seq_type_id")
    }

    changeSet(author: "borufka (generated)", id: "1557818756576-88") {
        addPrimaryKey(tableName: "ega_platform_model_seq_platform_model_label", columnNames: "ega_platform_model_seq_platform_model_labels_id,seq_platform_model_label_id")
    }
}
