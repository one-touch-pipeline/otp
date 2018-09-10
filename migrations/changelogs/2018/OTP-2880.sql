CREATE TABLE single_cell_bam_file_seq_track (
    single_cell_bam_file_seq_tracks_id bigint NOT NULL,
    seq_track_id bigint NOT NULL
);

Create UNIQUE INDEX single_cell_bam_file_seq_track_scbfst_id_unqiue_idx
    ON single_cell_bam_file_seq_track (single_cell_bam_file_seq_tracks_id, seq_track_id);

ALTER TABLE single_cell_bam_file_seq_track
    ADD CONSTRAINT single_cell_bam_file_seq_track_scbfst_id_fkey FOREIGN KEY (single_cell_bam_file_seq_tracks_id) REFERENCES abstract_bam_file(id);

ALTER TABLE single_cell_bam_file_seq_track
    ADD CONSTRAINT single_cell_bam_file_seq_track_seq_track_id_fkey FOREIGN KEY (seq_track_id) REFERENCES seq_track(id);