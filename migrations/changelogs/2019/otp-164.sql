-- update IDs in sample_type_per_project
WITH st (name, id, ct, rn) AS (
    SELECT lower(name),
           id,
           count(*)
           OVER (PARTITION BY lower(name)),
           row_number() OVER (PARTITION BY lower(name) ORDER BY id)
    FROM sample_type
),
     stm (name, new_id, old_id) AS (
         SELECT st_new.name, st_new.id, st_old.id
         FROM st AS st_new
                  JOIN st AS st_old ON st_new.name = st_old.name
         WHERE st_new.ct > 1
           AND st_new.rn = 1
           AND st_old.rn > 1
     )
UPDATE sample_type_per_project
SET sample_type_id = stm.new_id
FROM stm
WHERE sample_type_id = stm.old_id;

-- update IDs in processing_thresholds
WITH st (name, id, ct, rn) AS (
    SELECT lower(name),
           id,
           count(*)
           OVER (PARTITION BY lower(name)),
           row_number() OVER (PARTITION BY lower(name) ORDER BY id)
    FROM sample_type
),
     stm (name, new_id, old_id) AS (
         SELECT st_new.name, st_new.id, st_old.id
         FROM st AS st_new
                  JOIN st AS st_old ON st_new.name = st_old.name
         WHERE st_new.ct > 1
           AND st_new.rn = 1
           AND st_old.rn > 1
     )
UPDATE processing_thresholds
SET sample_type_id = stm.new_id
FROM stm
WHERE sample_type_id = stm.old_id;

-- update IDs in reference_genome_project_seq_type
WITH st (name, id, ct, rn) AS (
    SELECT lower(name),
           id,
           count(*)
           OVER (PARTITION BY lower(name)),
           row_number() OVER (PARTITION BY lower(name) ORDER BY id)
    FROM sample_type
),
     stm (name, new_id, old_id) AS (
         SELECT st_new.name, st_new.id, st_old.id
         FROM st AS st_new
                  JOIN st AS st_old ON st_new.name = st_old.name
         WHERE st_new.ct > 1
           AND st_new.rn = 1
           AND st_old.rn > 1
     )
UPDATE reference_genome_project_seq_type
SET sample_type_id = stm.new_id
FROM stm
WHERE sample_type_id = stm.old_id;


-- create new samples if necessary
WITH st (name, id, ct, rn) AS (
    SELECT lower(name),
           id,
           count(*)
           OVER (PARTITION BY lower(name)),
           row_number() OVER (PARTITION BY lower(name) ORDER BY id)
    FROM sample_type
),
     stm (name, new_id, old_id, rn) AS (
         SELECT st_new.name, st_new.id, st_old.id, st_old.rn
         FROM st AS st_new
                  JOIN st AS st_old ON st_new.name = st_old.name
         WHERE st_new.ct > 1
           AND st_new.rn = 1
           AND st_old.rn > 1
     )
INSERT INTO sample (id, version, individual_id, sample_type_id, date_created, last_updated)
SELECT nextval('hibernate_sequence'), 0, s.individual_id, stm.new_id, now(), now()
FROM stm
         JOIN sample AS s
              ON stm.old_id = s.sample_type_id
WHERE NOT exists(SELECT * FROM sample AS se WHERE se.sample_type_id = stm.new_id AND se.individual_id = s.individual_id);

-- update IDs in tables referencing sample
WITH st (name, id, ct, rn) AS (
    SELECT lower(name),
           id,
           count(*)
           OVER (PARTITION BY lower(name)),
           row_number() OVER (PARTITION BY lower(name) ORDER BY id)
    FROM sample_type
),
     sample_map (name, new_id, old_id) AS (
         SELECT st_new.name, s_new.id, s_old.id
         FROM st AS st_new
                  JOIN st AS st_old ON st_new.name = st_old.name
                  JOIN sample AS s_old ON s_old.sample_type_id = st_old.id
                  JOIN sample AS s_new ON s_new.sample_type_id = st_new.id AND s_new.individual_id = s_old.individual_id
         WHERE st_new.ct > 1
           AND st_new.rn = 1
           AND st_old.rn > 1
     )
UPDATE seq_scan
SET sample_id = sample_map.new_id
FROM sample_map
WHERE sample_id = sample_map.old_id;
;
WITH st (name, id, ct, rn) AS (
    SELECT lower(name),
           id,
           count(*)
           OVER (PARTITION BY lower(name)),
           row_number() OVER (PARTITION BY lower(name) ORDER BY id)
    FROM sample_type
),
     sample_map (name, new_id, old_id) AS (
         SELECT st_new.name, s_new.id, s_old.id
         FROM st AS st_new
                  JOIN st AS st_old ON st_new.name = st_old.name
                  JOIN sample AS s_old ON s_old.sample_type_id = st_old.id
                  JOIN sample AS s_new ON s_new.sample_type_id = st_new.id AND s_new.individual_id = s_old.individual_id
         WHERE st_new.ct > 1
           AND st_new.rn = 1
           AND st_old.rn > 1
     )
UPDATE seq_track
SET sample_id = sample_map.new_id
FROM sample_map
WHERE sample_id = sample_map.old_id;
;
WITH st (name, id, ct, rn) AS (
    SELECT lower(name),
           id,
           count(*)
           OVER (PARTITION BY lower(name)),
           row_number() OVER (PARTITION BY lower(name) ORDER BY id)
    FROM sample_type
),
     sample_map (name, new_id, old_id) AS (
         SELECT st_new.name, s_new.id, s_old.id
         FROM st AS st_new
                  JOIN st AS st_old ON st_new.name = st_old.name
                  JOIN sample AS s_old ON s_old.sample_type_id = st_old.id
                  JOIN sample AS s_new ON s_new.sample_type_id = st_new.id AND s_new.individual_id = s_old.individual_id
         WHERE st_new.ct > 1
           AND st_new.rn = 1
           AND st_old.rn > 1
     )
UPDATE sample_submission_object
SET sample_id = sample_map.new_id
FROM sample_map
WHERE sample_id = sample_map.old_id;
;
WITH st (name, id, ct, rn) AS (
    SELECT lower(name),
           id,
           count(*)
           OVER (PARTITION BY lower(name)),
           row_number() OVER (PARTITION BY lower(name) ORDER BY id)
    FROM sample_type
),
     sample_map (name, new_id, old_id) AS (
         SELECT st_new.name, s_new.id, s_old.id
         FROM st AS st_new
                  JOIN st AS st_old ON st_new.name = st_old.name
                  JOIN sample AS s_old ON s_old.sample_type_id = st_old.id
                  JOIN sample AS s_new ON s_new.sample_type_id = st_new.id AND s_new.individual_id = s_old.individual_id
         WHERE st_new.ct > 1
           AND st_new.rn = 1
           AND st_old.rn > 1
     )
UPDATE sample_identifier
SET sample_id = sample_map.new_id
FROM sample_map
WHERE sample_id = sample_map.old_id;
;
WITH st (name, id, ct, rn) AS (
    SELECT lower(name),
           id,
           count(*)
           OVER (PARTITION BY lower(name)),
           row_number() OVER (PARTITION BY lower(name) ORDER BY id)
    FROM sample_type
),
     sample_map (name, new_id, old_id) AS (
         SELECT st_new.name, s_new.id, s_old.id
         FROM st AS st_new
                  JOIN st AS st_old ON st_new.name = st_old.name
                  JOIN sample AS s_old ON s_old.sample_type_id = st_old.id
                  JOIN sample AS s_new ON s_new.sample_type_id = st_new.id AND s_new.individual_id = s_old.individual_id
         WHERE st_new.ct > 1
           AND st_new.rn = 1
           AND st_old.rn > 1
     )
UPDATE abstract_merging_work_package
SET sample_id = sample_map.new_id
FROM sample_map
WHERE sample_id = sample_map.old_id;
;

-- delete samples with old IDs
WITH st (name, id, ct, rn) AS (
    SELECT lower(name),
           id,
           count(*)
           OVER (PARTITION BY lower(name)),
           row_number() OVER (PARTITION BY lower(name) ORDER BY id)
    FROM sample_type
),
     stm (name, new_id, old_id) AS (
         SELECT st_new.name, st_new.id, st_old.id
         FROM st AS st_new
                  JOIN st AS st_old ON st_new.name = st_old.name
         WHERE st_new.ct > 1
           AND st_new.rn = 1
           AND st_old.rn > 1
     )
DELETE FROM sample
WHERE sample_type_id IN (
    SELECT stm.old_id
    FROM stm
);

-- delete duplicated rows from sample_type
WITH st (name, id, ct, rn) AS (
    SELECT lower(name),
           id,
           count(*)
           OVER (PARTITION BY lower(name)),
           row_number() OVER (PARTITION BY lower(name) ORDER BY id)
    FROM sample_type
),
     stm (name, new_id, old_id) AS (
         SELECT st_new.name, st_new.id, st_old.id
         FROM st AS st_new
                  JOIN st AS st_old ON st_new.name = st_old.name
         WHERE st_new.ct > 1
           AND st_new.rn = 1
           AND st_old.rn > 1
     )
DELETE
FROM sample_type
WHERE id IN (
    SELECT old_id
    FROM stm
);
