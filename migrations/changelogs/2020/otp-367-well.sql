UPDATE seq_track
SET cell_position = NULL
WHERE cell_position LIKE 'G%'
   OR cell_position LIKE 'H%'
   OR cell_position LIKE 'J%'
   OR cell_position LIKE 'S%';
