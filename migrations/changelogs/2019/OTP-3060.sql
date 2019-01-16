ALTER TABLE seq_track
  ADD COLUMN "cell_position" VARCHAR(255);

ALTER TABLE seq_track
  ADD CONSTRAINT seq_track_cell_position_run_id_laneId_key UNIQUE (cell_position, run_id, lane_id);
