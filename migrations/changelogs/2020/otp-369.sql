ALTER TABLE seq_track ADD COLUMN sample_identifier VARCHAR(255);

-- fill sample_identifier
UPDATE seq_track st
SET sample_identifier = subquery.sample_id
FROM (SELECT
            df.seq_track_id,
            -- meta_data_entry.value:
            -- the unfiltered meta_data_entries can hold an ODCF-guide "import identifier"
            -- import identifiers look like "(project)(individual)(sampleType)(sampleId)"
            -- or more recently, with [] instead of ().
            -- For our migration, we are only interested in the SampleID-part, so we extract only that.
            regexp_replace(regexp_replace(meta_data_entry.value,
                '\[.+\]\[.+\]\[.+\]\[(.+)\]', '\1'),
                '\(.+\)\(.+\)\(.+\)\((.+)\)', '\1'
            ) AS sample_id
      FROM meta_data_entry
            JOIN data_file df ON meta_data_entry.data_file_id = df.id
            JOIN meta_data_key mdk ON meta_data_entry.key_id = mdk.id
      WHERE (mdk.name = 'SAMPLE_ID' OR mdk.name = 'SAMPLE_NAME') -- mutually exclusive, changed as part of otp-569: 'SAMPLE_NAME alias'
) AS subquery
WHERE st.id = subquery.seq_track_id;

-- Delete all the sample_identifiers that were migrated. ANY seqtrack with same sample_identifier string will do,
-- even if it belongs to another sample (e.g. as happens after sample/lane swaps)
DELETE FROM sample_identifier si
WHERE exists(
    SELECT st.id FROM seq_track st
    WHERE st.sample_identifier = si.name
);

-- delete all unused sampleIdentifiers in 'closed' projects, where no new data is expected.
DELETE FROM sample_identifier si
            USING sample s2, individual i2, project p
WHERE
    si.sample_id = s2.id AND
    s2.individual_id = i2.id AND
    i2.project_id = p.id AND
    p.closed = true;

-- sample swap survivors --
-- Any sample that has a swap in its history counts as "all bets are off"
-- with regards to SampleIdentifier, just throw them away.
-- (compare with issue description, which also says all associated
--  SampleIdentifier objects should be deleted upon swap)
-- There isn't a hard-and-fast rule for swaps, and indeed some aren't documented
-- (in the DB) at all, but we can look for some specific text variations on 'moved',
-- 'swap' and 'changed' in strings.

-- 'ancient' style: swap documented in sample_identifier name
DELETE FROM sample_identifier si
    USING sample s2, individual i2
WHERE
        si.sample_id = s2.id AND
        s2.individual_id = i2.id AND
        (
                si.name like '% moved %' OR
                si.name like '% was changed %'
        );

-- 'newer' style: swap documented in comments
DELETE FROM sample_identifier si
        USING sample s2, individual i2, comment c
        WHERE
            si.sample_id = s2.id AND
            s2.individual_id = i2.id AND
            i2.comment_id = c.id AND
            (
                c.comment like '%== Lane swap - %' OR
                c.comment like '%== Sample swap - %' OR
                c.comment like '%== Individual swap - %' OR
                c.comment like '% was changed %'
            );


-- Delete old leftovers
-- If they haven't been used in 3 months, they're probably spurious.
DELETE FROM sample_identifier WHERE date_created <= '2020-02-29';

-- Make it an error to create SeqTracks without sampleIdentifiers in the future;
ALTER TABLE seq_track ALTER COLUMN sample_identifier SET NOT NULL;
