UPDATE
  public.seq_track
SET
  alignment_state='NOT_STARTED'
WHERE
  id IN
  (
    SELECT
      alignment_pass.seq_track_id
    FROM
      public.alignment_pass
    WHERE
      id::text IN
      (
        SELECT
          value
        FROM
          public.process_parameter
        WHERE
          process_id IN
          (
            SELECT
              process.id
            FROM
              processing_step,
              process
            WHERE
              process.id=processing_step.process_id
              AND
              start_job_class='de.dkfz.tbi.otp.job.jobs.alignment.BwaAlignmentStartJob'
              AND NOT EXISTS
              (
                  SELECT
                    *
                  FROM
                    processing_step_update
                  WHERE
                    processing_step.id=processing_step_update.processing_step_id
              )
          )
      )
  );


UPDATE
  public.seq_track
SET
  fastqc_state='NOT_STARTED'
WHERE
  id::text IN
  (
    SELECT
      value
    FROM
      public.process_parameter
    WHERE
      process_id IN
      (
        SELECT
          process.id
        FROM
          processing_step,
          process
        WHERE
          process.id=processing_step.process_id
          AND
          start_job_class='de.dkfz.tbi.otp.job.jobs.fastqc.FastqcStartJob'
          AND NOT EXISTS
          (
              SELECT
                *
              FROM
                processing_step_update
              WHERE
                processing_step.id=processing_step_update.processing_step_id
          )
      )
  );


DELETE
FROM
  public.process_parameter
WHERE
  process_parameter.process_id IN
  (
    SELECT
      process_id
    FROM
      processing_step
    WHERE NOT EXISTS
    (
        SELECT
          *
        FROM
          processing_step_update
        WHERE
          processing_step.id=processing_step_update.processing_step_id
    )
  );


DELETE
FROM
  public.processing_step
WHERE
  process_id IN
  (
    SELECT
      process_id
    FROM
      processing_step
    WHERE NOT EXISTS
    (
        SELECT
          *
        FROM
          processing_step_update
        WHERE
          processing_step.id=processing_step_update.processing_step_id
    )
  );


DELETE
FROM
  public.process
WHERE
  NOT EXISTS
  (
      SELECT
        *
      FROM
        processing_step
      WHERE
        processing_step.process_id=process.id
  );
