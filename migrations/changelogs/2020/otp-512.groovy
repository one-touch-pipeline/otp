databaseChangeLog = {
    changeSet(author: "", id: "1589897548686-1") {
        addColumn(tableName: "cluster_job") {
            column(name: "workflow_step_id", type: "int8")
        }
    }

    changeSet(author: "", id: "1589897548686-4") {
        createIndex(indexName: "cluster_job_workflow_step_idx", tableName: "cluster_job") {
            column(name: "workflow_step_id")
        }
    }

    changeSet(author: "", id: "1589897548686-5") {
        addForeignKeyConstraint(baseColumnNames: "workflow_step_id", baseTableName: "cluster_job", constraintName: "FKb09t20osg9k84vx49cvo9ud1u", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_step")
    }

    changeSet(author: "", id: "1589897548686-17") {
        dropForeignKeyConstraint(baseTableName: "workflow_step_cluster_job", constraintName: "workflow_step_cluster_job_cluster_job_id_fkey")
    }

    changeSet(author: "", id: "1589897548686-18") {
        dropForeignKeyConstraint(baseTableName: "workflow_step_cluster_job", constraintName: "workflow_step_cluster_job_workflow_step_cluster_jobs_id_fkey")
    }

    changeSet(author: "", id: "1589897548686-25") {
        dropTable(tableName: "workflow_step_cluster_job")
    }

    changeSet(author: "", id: "1589897548686-66") {
        dropNotNullConstraint(columnDataType: "bigint", columnName: "processing_step_id", tableName: "cluster_job")
    }
}
