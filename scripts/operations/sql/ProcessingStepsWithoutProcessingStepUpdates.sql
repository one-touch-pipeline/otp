SELECT
  *
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
