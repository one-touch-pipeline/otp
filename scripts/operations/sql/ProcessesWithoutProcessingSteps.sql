SELECT
  *
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
