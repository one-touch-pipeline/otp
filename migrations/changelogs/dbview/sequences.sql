BEGIN;

DROP VIEW IF EXISTS sequences;

CREATE VIEW sequences AS

SELECT

st.id AS seq_track_id,
st.seq_type_id,
r.seq_platform_id,
sp.seq_platform_model_label_id,
sp.sequencing_kit_label_id,
st.sample_id,
st.run_id,
st.pipeline_version_id,
ilse.ilse_number AS ilse_id,
r.seq_center_id,
s.sample_type_id,
s.individual_id,
p.id AS project_id,
re.id AS realm_id,

st.quality_encoding,
st.fastqc_state,
st.n_base_pairs,
st.lane_id,
st.library_name,
st.insert_size,
st.has_original_bam,
st.problem,
st.single_cell_well_label,
lpk.short_display_name AS library_preparation_kit,
at.name AS antibody_target,
r.name,
r.date_executed,
r.date_created,
r.blacklisted,
sp.name AS seq_platform_name,
spml.name AS seq_platform_model_label_name,
skl.name AS sequencing_kit_label_name,
seqType.name AS seq_type_name,
seqType.display_name AS seq_type_display_name,
seqType.library_layout,
seqType.dir_name,
seqType.single_cell,
sampleType.name AS sample_type_name,
i.type,
i.pid,
i.mock_pid,
i.mock_full_name,
re.name as realm_name,
p.name AS project_name,
p.dir_name AS project_dir_name,
sc.name AS seq_center_name,
sc.dir_name AS seq_center_dir_name,
COALESCE((select bool_and(file_exists) from data_file df where df.seq_track_id = st.id), false) as file_exists,
COALESCE((select bool_and(file_withdrawn) from data_file df where df.seq_track_id = st.id), false) as file_withdrawn

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
INNER JOIN realm as re
on p.realm_id = re.id
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
;

COMMIT;
