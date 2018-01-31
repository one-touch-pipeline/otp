BEGIN;

DROP VIEW IF EXISTS aggregate_sequences;


CREATE VIEW aggregate_sequences AS SELECT

(sum(st.n_base_pairs))::bigint as sum_n_base_pairs,
(floor(sum(st.n_base_pairs) / 1e9))::bigint as sum_n_base_pairs_gb,
(count(st.id))::bigint AS lane_count,

st.seq_type_id,
r.seq_platform_id,
sp.seq_platform_model_label_id,
sp.sequencing_kit_label_id,
st.sample_id,
r.seq_center_id,
s.sample_type_id,
s.individual_id,
p.id AS project_id,
re.id AS realm_id,

sp.name AS seq_platform_name,
spml.name AS seq_platform_model_label_name,
skl.name AS sequencing_kit_label_name,
seqType.name AS seq_type_name,
seqType.display_name AS seq_type_display_name,
seqType.library_layout,
seqType.dir_name,
sampleType.name AS sample_type_name,
i.type,
i.pid,
i.mock_pid,
i.mock_full_name,
re.name as realm_name,
p.name AS project_name,
p.dir_name AS project_dir_name,
sc.name AS seq_center_name,
sc.dir_name AS seq_center_dir_name

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
LEFT OUTER JOIN seq_platform_model_label spml
ON sp.seq_platform_model_label_id = spml.id
LEFT OUTER JOIN sequencing_kit_label skl
ON sp.sequencing_kit_label_id = skl.id

WHERE st.id NOT IN (
SELECT DISTINCT seq.id from seq_track as seq
INNER JOIN data_file AS df ON df.seq_track_id = seq.id
WHERE df.file_withdrawn != false
)

group by

st.seq_type_id,
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
sampleType.name,
i.type,
i.pid,
i.mock_pid,
i.mock_full_name,
p.name,
p.dir_name,
re.name,
sc.name,
sc.dir_name;

COMMIT;
