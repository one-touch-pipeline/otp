/*
 * Copyright 2011-2024 The OTP authors
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

SELECT seqTrack.id                                                                                                      AS seq_track_id,
       seqTrack.seq_type_id,
       run.seq_platform_id,
       seqPlatform.seq_platform_model_label_id,
       seqPlatform.sequencing_kit_label_id,
       seqTrack.sample_id,
       seqTrack.run_id,
       seqTrack.pipeline_version_id,
       ilse.ilse_number                                                                                                 AS ilse_id,
       run.seq_center_id,
       sample.sample_type_id,
       sample.individual_id,
       project.id                                                                                                       AS project_id,

       seqTrack.quality_encoding,
       seqTrack.fastqc_state,
       seqTrack.n_base_pairs,
       seqTrack.lane_id,
       seqTrack.library_name,
       seqTrack.insert_size,
       seqTrack.has_original_bam,
       seqTrack.problem,
       seqTrack.single_cell_well_label,
       seqTrack.sample_identifier                                                                                       AS sample_name,
       seqTrack.date_created                                                                                            AS import_date,
       libPrepKit.name                                                                                                  AS library_preparation_kit,
       antibodyTarget.name                                                                                              AS antibody_target,
       run.name,
       run.date_executed,
       run.date_created,
       run.blacklisted,
       seqPlatform.name                                                                                                 AS seq_platform_name,
       spml.name                                                                                                        AS seq_platform_model_label_name,
       seqKitLbl.name                                                                                                   AS sequencing_kit_label_name,
       seqType.name                                                                                                     AS seq_type_name,
       seqType.display_name                                                                                             AS seq_type_display_name,
       seqType.library_layout,
       seqType.dir_name,
       seqType.single_cell,
       sampleType.name                                                                                                  AS sample_type_name,
       individual.type,
       individual.pid,
       project.name                                                                                                     AS project_name,
       project.dir_name                                                                                                 AS project_dir_name,
       project.state = 'ARCHIVED'                                                                                       AS file_archived,
       seqCenter.name                                                                                                   AS seq_center_name,
       seqCenter.dir_name                                                                                               AS seq_center_dir_name,
       COALESCE((SELECT BOOL_AND(file_exists) FROM raw_sequence_file sf WHERE sf.seq_track_id = seqTrack.id), FALSE)    AS file_exists,
       COALESCE((SELECT BOOL_AND(file_withdrawn) FROM raw_sequence_file sf WHERE sf.seq_track_id = seqTrack.id), FALSE) AS file_withdrawn,
       scn.name                                                                                                         AS species_common_name,
       species.scientific_name                                                                                          AS scientific_name,
       strn.name                                                                                                        AS strain,
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
        WHERE ssws.sample_mixed_in_species_id = sample.id
        GROUP BY ssws.sample_mixed_in_species_id)                                                                       AS mixed_in_species
FROM seq_track AS seqTrack
         INNER JOIN run
                    ON run.id = seqTrack.run_id
         INNER JOIN seq_platform AS seqPlatform
                    ON seqPlatform.id = run.seq_platform_id
         INNER JOIN seq_type AS seqType
                    ON seqType.id = seqTrack.seq_type_id
         INNER JOIN sample
                    ON sample.id = seqTrack.sample_id
         INNER JOIN sample_type AS sampleType
                    ON sample.sample_type_id = sampleType.id
         INNER JOIN individual
                    ON sample.individual_id = individual.id
         INNER JOIN project
                    ON individual.project_id = project.id
         INNER JOIN seq_center AS seqCenter
                    ON run.seq_center_id = seqCenter.id
         LEFT OUTER JOIN ilse_submission ilse
                         ON seqTrack.ilse_submission_id = ilse.id
         LEFT OUTER JOIN seq_platform_model_label spml
                         ON seqPlatform.seq_platform_model_label_id = spml.id
         LEFT OUTER JOIN sequencing_kit_label seqKitLbl
                         ON seqPlatform.sequencing_kit_label_id = seqKitLbl.id
         LEFT OUTER JOIN library_preparation_kit libPrepKit
                         ON seqTrack.library_preparation_kit_id = libPrepKit.id
         LEFT OUTER JOIN antibody_target antibodyTarget
                         ON seqTrack.antibody_target_id = antibodyTarget.id
         LEFT OUTER JOIN species_with_strain sws
                         ON individual.species_id = sws.id
         LEFT OUTER JOIN species
                         ON sws.species_id = species.id
         LEFT OUTER JOIN species_common_name scn
                         ON scn.id = species.species_common_name_id
         LEFT OUTER JOIN strain strn
                         ON strn.id = sws.strain_id
;
