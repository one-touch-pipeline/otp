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

WITH alignable_seq_type(id, name) AS (
    SELECT id, name
    FROM seq_type
    WHERE (name IN (
                    'WHOLE_GENOME',
                    'EXON',
                    'WHOLE_GENOME_BISULFITE',
                    'WHOLE_GENOME_BISULFITE_TAGMENTATION',
                    'RNA',
                    'ChIP Seq') AND library_layout = 'PAIRED' AND single_cell = FALSE)
       OR (name = 'RNA' AND library_layout = 'SINGLE' AND single_cell = FALSE)
       OR (name = '10x_scRNA' AND library_layout = 'PAIRED' AND single_cell = TRUE)
),
     all_project(id) AS (
         SELECT id
         FROM project
     )
INSERT
INTO merging_criteria (id, version, project_id, seq_type_id, use_lib_prep_kit, use_seq_platform_group, date_created, last_updated)
SELECT nextval('hibernate_sequence'),
       0,
       all_project.id,
       alignable_seq_type.id,
       CASE WHEN alignable_seq_type.name IN ('WHOLE_GENOME_BISULFITE', 'WHOLE_GENOME_BISULFITE_TAGMENTATION') THEN FALSE ELSE TRUE END,
       'USE_OTP_DEFAULT',
       NOW(),
       NOW()
FROM alignable_seq_type
         CROSS JOIN all_project
WHERE NOT EXISTS(
        SELECT id FROM merging_criteria WHERE project_id = all_project.id AND seq_type_id = alignable_seq_type.id
    );
