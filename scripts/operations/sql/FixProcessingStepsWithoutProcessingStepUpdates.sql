-- Copyright 2011-2024 The OTP authors
--
-- Permission is hereby granted, free of charge, to any person obtaining a copy
-- of this software and associated documentation files (the "Software"), to deal
-- in the Software without restriction, including without limitation the rights
-- to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
-- copies of the Software, and to permit persons to whom the Software is
-- furnished to do so, subject to the following conditions:
--
-- The above copyright notice and this permission notice shall be included in all
-- copies or substantial portions of the Software.
--
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
-- IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
-- FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
-- AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
-- LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
-- OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
-- SOFTWARE.

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
