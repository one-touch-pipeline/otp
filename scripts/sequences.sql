CREATE VIEW sequences AS

SELECT

st.id AS seq_track_id,
st.seq_type_id,
st.seq_platform_id,
st.sample_id,
st.run_id,
st.pipeline_version_id,
r.seq_center_id,
s.sample_type_id,
s.individual_id,
p.id AS project_id,
st.quality_encoding,
st.fastqc_state,
st.alignment_state,
st.using_original_bam,
st.n_reads,
st.n_base_pairs,
st.lane_id,
st.insert_size,
st.has_original_bam,
st.has_final_bam,
r.quality_evaluated,
r.data_quality,
r.storage_realm,
r.name,
r.multiple_source,
r.date_executed,
r.date_created,
r.blacklisted,
sp.name AS seq_platform_name,
sp.model,
seqType.name AS seq_type_name,
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
