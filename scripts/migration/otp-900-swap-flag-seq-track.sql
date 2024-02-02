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

-- Update swapped flag and sample_identifier based on comment, metadata entry and sample_identifier values
WITH query (seq_trk_id) AS (
    SELECT DISTINCT sq.seq_trk_id
    FROM (SELECT seq_track.id AS seq_trk_id
          FROM seq_track
          WHERE seq_track.sample_identifier LIKE '%was changed on%'
          UNION
          SELECT seq_track_id AS seq_trk_id
          FROM seq_track
                   JOIN data_file ON data_file.seq_track_id = seq_track.id
                   JOIN comment ON comment.id = data_file.comment_id
          WHERE comment.comment LIKE '%Attention: Datafile swapped!%'
          UNION
          SELECT seq_track_id AS seq_trk_id
          FROM seq_track
                   JOIN data_file ON data_file.seq_track_id = seq_track.id
                   JOIN meta_data_entry ON meta_data_entry.data_file_id = data_file.id
          WHERE meta_data_entry.value LIKE '%was changed on%') AS sq
)
UPDATE seq_track
SET swapped = TRUE,
    sample_identifier = regexp_replace(sample_identifier, ' was changed on.*', '')
FROM query
WHERE query.seq_trk_id = seq_track.id
