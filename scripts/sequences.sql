CREATE VIEW sequences AS

SELECT

st.id AS seq_track_id,
st.seq_type_id,
st.seq_platform_id,
sp.seq_platform_model_label_id,
sp.sequencing_kit_label_id,
st.sample_id,
st.run_id,
st.pipeline_version_id,
r.seq_center_id,
s.sample_type_id,
s.individual_id,
p.id AS project_id,

st.quality_encoding,
st.fastqc_state,
st.using_original_bam,
st.n_base_pairs,
st.lane_id,
st.library_name,
st.insert_size,
st.has_original_bam,
st.has_final_bam,
r.storage_realm,
r.name,
r.multiple_source,
r.date_executed,
r.date_created,
r.blacklisted,
sp.name AS seq_platform_name,
spml.name AS seq_platform_model_label_name,
skl.name AS sequencing_kit_label_name,
seqType.name AS seq_type_name,
seqType.alias AS seq_type_alias,
(CASE WHEN seqType.alias IS NOT NULL THEN seqType.alias ELSE seqType.name END) AS seq_type_display_name,
seqType.library_layout,
seqType.dir_name,
sampleType.name AS sample_type_name,
i.type,
i.pid,
i.mock_pid,
i.mock_full_name,
p.realm_name,
p.name AS project_name,
p.dir_name AS project_dir_name,
sc.name AS seq_center_name,
sc.dir_name AS seq_center_dir_name

FROM seq_track AS st
INNER JOIN run AS r
ON r.id = st.run_id
INNER JOIN seq_platform AS sp
ON sp.id = st.seq_platform_id
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
INNER JOIN seq_center AS sc
ON r.seq_center_id = sc.id
LEFT OUTER JOIN seq_platform_model_label spml
ON sp.seq_platform_model_label_id = spml.id
LEFT OUTER JOIN sequencing_kit_label skl
ON sp.sequencing_kit_label_id = skl.id;
