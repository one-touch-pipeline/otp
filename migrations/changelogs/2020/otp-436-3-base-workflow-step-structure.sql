ALTER TABLE workflow_step
    ADD COLUMN workflow_run_id BIGINT NOT NULL,
    ADD CONSTRAINT workflow_step_workflow_run_id_fk FOREIGN KEY (workflow_run_id) REFERENCES workflow_run(id),

    ADD COLUMN bean_name VARCHAR(255) NOT NULL,

    ADD COLUMN wes_identifier VARCHAR(255),

    ADD COLUMN state VARCHAR(255) NOT NULL,

    ADD COLUMN workflow_error_id BIGINT,
    ADD CONSTRAINT workflow_step_workflow_error_fk FOREIGN KEY (workflow_error_id) REFERENCES workflow_error(id),

    ADD COLUMN previous_id BIGINT,
    ADD CONSTRAINT workflow_step_previous_fk FOREIGN KEY (previous_id) REFERENCES workflow_step(id),

    ADD COLUMN restarted_from_id BIGINT,
    ADD CONSTRAINT workflow_step_restarted_from_fk FOREIGN KEY (restarted_from_id) REFERENCES workflow_step(id),

    ADD COLUMN obsolete BOOLEAN NOT NULL,

    ADD COLUMN comment_id BIGINT,
    ADD CONSTRAINT workflow_step_comment_fk FOREIGN KEY (comment_id) REFERENCES comment(id),

    ADD COLUMN workflow_steps_idx INTEGER;

CREATE INDEX workflow_step_workflow_run_id_idx on workflow_step(workflow_run_id);
CREATE INDEX workflow_step_workflow_error_id_idx on workflow_step(workflow_error_id);
CREATE INDEX workflow_step_previous_id_idx on workflow_step(previous_id);
CREATE INDEX workflow_step_restarted_from_id_idx on workflow_step(restarted_from_id);
CREATE INDEX workflow_step_comment_id_idx on workflow_step(comment_id);


-- Set<ClusterJob>
CREATE TABLE workflow_step_cluster_job(
    workflow_step_cluster_jobs_id BIGINT NOT NULL,
    FOREIGN KEY (workflow_step_cluster_jobs_id) REFERENCES workflow_step(id),
    cluster_job_id BIGINT,
    FOREIGN KEY (cluster_job_id) REFERENCES cluster_job(id)
);

CREATE INDEX workflow_step_cluster_job_workflow_step_cluster_jobs_id_idx on workflow_step_cluster_job(workflow_step_cluster_jobs_id);
CREATE INDEX workflow_step_cluster_job_cluster_job_id_idx on workflow_step_cluster_job(cluster_job_id);

-- List<WorkflowLog>
CREATE TABLE workflow_step_workflow_log(
    workflow_step_logs_id BIGINT NOT NULL,
    FOREIGN KEY (workflow_step_logs_id) REFERENCES workflow_step(id),
    workflow_log_id BIGINT,
    FOREIGN KEY (workflow_log_id) REFERENCES workflow_log(id),
    logs_idx INTEGER
);

CREATE INDEX workflow_step_workflow_log_workflow_step_logs_id_idx on workflow_step_workflow_log(workflow_step_logs_id);
CREATE INDEX workflow_step_workflow_log_workflow_log_id_idx on workflow_step_workflow_log(workflow_log_id);

-- this table is not needed, it is mapped via the idx in workflow_step
DROP TABLE workflow_run_workflow_step;