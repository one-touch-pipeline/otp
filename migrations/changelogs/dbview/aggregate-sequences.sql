/*
 * Copyright 2011-2022 The OTP authors
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

DROP VIEW IF EXISTS aggregate_sequences;


CREATE VIEW aggregate_sequences AS
SELECT (SUM(st.n_base_pairs))::bigint              AS sum_n_base_pairs,
       (FLOOR(SUM(st.n_base_pairs) / 1e9))::bigint AS sum_n_base_pairs_gb,
       (COUNT(st.id))::bigint                      AS lane_count,

       st.seq_type_id,
       r.seq_platform_id,
       sp.seq_platform_model_label_id,
       sp.sequencing_kit_label_id,
       st.sample_id,
       r.seq_center_id,
       s.sample_type_id,
       s.individual_id,
       p.id                                        AS project_id,
       re.id                                       AS realm_id,

       sp.name                                     AS seq_platform_name,
       spml.name                                   AS seq_platform_model_label_name,
       skl.name                                    AS sequencing_kit_label_name,
       seqType.name                                AS seq_type_name,
       seqType.display_name                        AS seq_type_display_name,
       seqType.library_layout,
       seqType.dir_name,
       seqType.single_cell,
       sampleType.name                             AS sample_type_name,
       i.type,
       i.pid,
       re.name                                     AS realm_name,
       p.name                                      AS project_name,
       p.dir_name                                  AS project_dir_name,
       sc.name                                     AS seq_center_name,
       sc.dir_name                                 AS seq_center_dir_name

FROM seq_track AS st
         INNER JOIN run AS r
                    ON r.id = st.run_id
         INNER JOIN seq_platform AS sp
                    ON sp.id = r.seq_platform_id
         INNER JOIN seq_type AS seqType
                    ON seqType.id = st.seq_type_id
         INNER JOIN sample AS s
                    ON s.id = st.sample_id
         INNER JOIN sample_type AS sampleType
                    ON s.sample_type_id = sampleType.id
         INNER JOIN individual AS i
                    ON s.individual_id = i.id
         INNER JOIN project AS p
                    ON i.project_id = p.id
         INNER JOIN realm AS re
                    ON p.realm_id = re.id
         INNER JOIN seq_center AS sc
                    ON r.seq_center_id = sc.id
         LEFT OUTER JOIN seq_platform_model_label spml
                         ON sp.seq_platform_model_label_id = spml.id
         LEFT OUTER JOIN sequencing_kit_label skl
                         ON sp.sequencing_kit_label_id = skl.id

WHERE st.id NOT IN (SELECT DISTINCT seq.id
                    FROM seq_track AS seq
                             INNER JOIN data_file AS df ON df.seq_track_id = seq.id
                    WHERE df.file_withdrawn != FALSE)

GROUP BY st.seq_type_id,
         r.seq_platform_id,
         sp.seq_platform_model_label_id,
         sp.sequencing_kit_label_id,
         st.sample_id,
         r.seq_center_id,
         s.sample_type_id,
         s.individual_id,
         p.id,
         re.id,
         sp.name,
         spml.name,
         skl.name,
         seqType.name,
         seqType.display_name,
         seqType.library_layout,
         seqType.dir_name,
         seqType.single_cell,
         sampleType.name,
         i.type,
         i.pid,
         p.name,
         p.dir_name,
         re.name,
         sc.name,
         sc.dir_name;
