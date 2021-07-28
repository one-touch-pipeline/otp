/*
 * Copyright 2011-2021 The OTP authors
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

-- add new column shortDisplayName
ALTER TABLE workflow_run ADD short_display_name varchar(255);

-- generate shortDisplayName for existing (data installation) workflows
DO
'
DECLARE
    wf_run record;
BEGIN
    FOR wf_run IN SELECT DISTINCT workflow_run.id AS wr_id, wa.produced_by_id AS wa_id, workflow_run.display_name, i.pid, st.name AS sample_type, t.name AS seq_type
                  FROM workflow_run
                           LEFT JOIN workflow_artefact wa ON workflow_run.id = wa.produced_by_id
                           LEFT JOIN individual i ON wa.individual_id = i.id
                           LEFT JOIN seq_track s ON wa.id = s.workflow_artefact_id
                           LEFT JOIN sample sa ON s.sample_id = sa.id
                           LEFT JOIN sample_type st ON sa.sample_type_id = st.id
                           LEFT JOIN seq_type t ON s.seq_type_id = t.id
        LOOP
            IF wf_run.wa_id IS NOT NULL THEN
                UPDATE workflow_run SET short_display_name = TRIM(CONCAT(''DI: '', wf_run.pid, '' '', wf_run.sample_type, '' '', wf_run.seq_type)) WHERE id = wf_run.wr_id;
            ELSE
                UPDATE workflow_run SET short_display_name = wf_run.display_name WHERE id = wf_run.wr_id;
            END IF;
        END LOOP;
END;
'  LANGUAGE PLPGSQL;

-- set new column to nullable: false
ALTER TABLE workflow_run ALTER COLUMN short_display_name SET NOT NULL;
