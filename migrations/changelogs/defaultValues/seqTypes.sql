--pancaner
INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'WHOLE_GENOME', 'PAIRED', 'whole_genome_sequencing', 'WGS', 'WGS', FALSE, FALSE, FALSE, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'EXON', 'PAIRED', 'exon_sequencing', 'EXOME', 'WES', FALSE, FALSE, FALSE, TRUE, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'WHOLE_GENOME_BISULFITE', 'PAIRED', 'whole_genome_bisulfite_sequencing', 'WGBS', 'WGBS', FALSE, FALSE, FALSE, FALSE,
        NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'WHOLE_GENOME_BISULFITE_TAGMENTATION', 'PAIRED', 'whole_genome_bisulfite_tagmentation_sequencing', 'WGBS_TAG',
        'WGBSTAG', FALSE, FALSE, FALSE, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'ChIP Seq', 'SINGLE', 'chip_seq_sequencing', 'ChIP', 'CHIPSEQ', FALSE, TRUE, FALSE, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'ChIP Seq', 'PAIRED', 'chip_seq_sequencing', 'ChIP', 'CHIPSEQ', FALSE, TRUE, FALSE, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

--rna alignment
INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'RNA', 'SINGLE', 'rna_sequencing', 'RNA', 'RNA', FALSE, FALSE, FALSE, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'RNA', 'PAIRED', 'rna_sequencing', 'RNA', 'RNA', FALSE, FALSE, FALSE, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

--cell ranger
INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, '10x_scRNA', 'PAIRED', '10x_scRNA_sequencing', '10x_scRNA', '', TRUE, FALSE, FALSE, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;
