databaseChangeLog = {

    changeSet(author: "kosnac (generated)", id: "1553007665529-4") {
        addForeignKeyConstraint(baseColumnNames: "comment_id", baseTableName: "process", constraintName: "FK504flhgywrloqkdr6u2gpjsrm", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "comment")
    }

    changeSet(author: "kosnac (generated)", id: "1553007665529-5") {
        addForeignKeyConstraint(baseColumnNames: "seq_type_id", baseTableName: "processing_thresholds", constraintName: "FK8o7rmi93crkr1h2au59gx2u3p", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_type")
    }

    changeSet(author: "kosnac (generated)", id: "1553007665529-6") {
        addForeignKeyConstraint(baseColumnNames: "sequencing_kit_label_id", baseTableName: "sequencing_kit_label_import_alias", constraintName: "FK8r5xo3dh3dw7k4nl3qjwcnfiq", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "sequencing_kit_label")
    }

    changeSet(author: "kosnac (generated)", id: "1553007665529-7") {
        addForeignKeyConstraint(baseColumnNames: "project_id", baseTableName: "user_project_role", constraintName: "FK9l1icyrvwmfc2lr9fskttk85b", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project")
    }

    changeSet(author: "kosnac (generated)", id: "1553007665529-8") {
        addForeignKeyConstraint(baseColumnNames: "project_id", baseTableName: "sample_type_per_project", constraintName: "FKakoxk94oy68igcpbvumsdpnst", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project")
    }

    changeSet(author: "kosnac (generated)", id: "1553007665529-9") {
        addForeignKeyConstraint(baseColumnNames: "seq_platform_model_label_id", baseTableName: "seq_platform_model_label_import_alias", constraintName: "FKamsibsqwqs8galdrsc7voj3q1", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_platform_model_label")
    }

    changeSet(author: "kosnac (generated)", id: "1553007665529-10") {
        addForeignKeyConstraint(baseColumnNames: "project_id", baseTableName: "processing_thresholds", constraintName: "FKb1j34pggqajkqbuch7ybw2su9", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project")
    }

    changeSet(author: "kosnac (generated)", id: "1553007665529-11") {
        addForeignKeyConstraint(baseColumnNames: "sample_type_id", baseTableName: "sample_type_per_project", constraintName: "FKdkn0ecyj5k85o5rwp1rfj8arm", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "sample_type")
    }

    changeSet(author: "kosnac (generated)", id: "1553007665529-12") {
        addForeignKeyConstraint(baseColumnNames: "seq_type_id", baseTableName: "seq_type_import_alias", constraintName: "FKe38r5kt3fs5scywq181ha194d", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_type")
    }
}