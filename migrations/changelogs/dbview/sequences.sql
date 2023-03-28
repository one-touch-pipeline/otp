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

DROP VIEW IF EXISTS sequences;

CREATE VIEW sequences AS

SELECT st.id                                                                                              AS seq_track_id,
       st.seq_type_id,
       r.seq_platform_id,
       sp.seq_platform_model_label_id,
       sp.sequencing_kit_label_id,
       st.sample_id,
       st.run_id,
       st.pipeline_version_id,
       ilse.ilse_number                                                                                   AS ilse_id,
       r.seq_center_id,
       s.sample_type_id,
       s.individual_id,
       p.id                                                                                               AS project_id,
       re.id                                                                                              AS realm_id,

       st.quality_encoding,
       st.fastqc_state,
       st.n_base_pairs,
       st.lane_id,
       st.library_name,
       st.insert_size,
       st.has_original_bam,
       st.problem,
       st.single_cell_well_label,
       lpk.name                                                                                           AS library_preparation_kit,
       at.name                                                                                            AS antibody_target,
       r.name,
       r.date_executed,
       r.date_created,
       r.blacklisted,
       sp.name                                                                                            AS seq_platform_name,
       spml.name                                                                                          AS seq_platform_model_label_name,
       skl.name                                                                                           AS sequencing_kit_label_name,
       seqType.name                                                                                       AS seq_type_name,
       seqType.display_name                                                                               AS seq_type_display_name,
       seqType.library_layout,
       seqType.dir_name,
       seqType.single_cell,
       sampleType.name                                                                                    AS sample_type_name,
       i.type,
       i.pid,
       re.name                                                                                            AS realm_name,
       p.name                                                                                             AS project_name,
       p.dir_name                                                                                         AS project_dir_name,
       p.archived                                                                                         AS file_archived,
       sc.name                                                                                            AS seq_center_name,
       sc.dir_name                                                                                        AS seq_center_dir_name,
       COALESCE((SELECT BOOL_AND(file_exists) FROM data_file df WHERE df.seq_track_id = st.id), FALSE)    AS file_exists,
       COALESCE((SELECT BOOL_AND(file_withdrawn) FROM data_file df WHERE df.seq_track_id = st.id), FALSE) AS file_withdrawn,
       scn.name                                                                                           AS species_common_name,
       spe.scientific_name                                                                                AS scientific_name,
       strn.name                                                                                          AS strain,
       (SELECT STRING_AGG(
                       (SELECT CONCAT(
                                       (SELECT name
                                        FROM species_common_name
                                        WHERE id = (SELECT species_common_name_id FROM species WHERE species.id = sws.species_id)),
                                       ' (', (SELECT scientific_name FROM species WHERE species.id = sws.species_id), ')',
                                       ' [', (SELECT name FROM strain WHERE strain.id = sws.strain_id), ']')
                        FROM species_with_strain sws
                        WHERE ssws.species_with_strain_id = sws.id), ' | ')
        FROM sample_species_with_strain ssws
        WHERE ssws.sample_mixed_in_species_id = s.id
        GROUP BY ssws.sample_mixed_in_species_id)                                                         AS mixed_in_species
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
         LEFT OUTER JOIN ilse_submission ilse
                         ON st.ilse_submission_id = ilse.id
         LEFT OUTER JOIN seq_platform_model_label spml
                         ON sp.seq_platform_model_label_id = spml.id
         LEFT OUTER JOIN sequencing_kit_label skl
                         ON sp.sequencing_kit_label_id = skl.id
         LEFT OUTER JOIN library_preparation_kit lpk
                         ON st.library_preparation_kit_id = lpk.id
         LEFT OUTER JOIN antibody_target at
                         ON st.antibody_target_id = at.id
         LEFT OUTER JOIN species_with_strain sws
                         ON i.species_id = sws.id
         LEFT OUTER JOIN species spe
                         ON sws.species_id = spe.id
         LEFT OUTER JOIN species_common_name scn
                         ON scn.id = spe.species_common_name_id
         LEFT OUTER JOIN strain strn
                         ON strn.id = sws.strain_id
;
