CREATE VIEW aggregate_sequences AS SELECT

sum(st.n_base_pairs) as sum_n_base_pairs,
floor(sum(st.n_base_pairs) / 1e9) as sum_n_base_pairs_gb,
count(st.id) AS lane_count,

st.seq_type_id,
st.seq_platform_id,
st.sample_id,
r.seq_center_id,
s.sample_type_id,
s.individual_id,
p.id AS project_id,

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

group by

st.seq_type_id,
st.seq_platform_id,
st.sample_id,
r.seq_center_id,
s.sample_type_id,
s.individual_id,
p.id,

sp.name,
sp.model,
seqType.name,
seqType.library_layout,
seqType.dir_name,
sampleType.name,
i.type,
i.pid,
i.mock_pid,
i.mock_full_name,
p.realm_name,
p.name,
p.dir_name,
sc.name,
sc.dir_name
