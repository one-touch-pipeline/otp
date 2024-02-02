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

-- pancancer
INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'WHOLE_GENOME', 'PAIRED', 'whole_genome_sequencing', 'WGS', 'WGS', FALSE, FALSE, FALSE, FALSE, NOW(),
        NOW())
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
VALUES (NEXTVAL('hibernate_sequence'), 0, 'ChIP Seq', 'SINGLE', 'chip_seq_sequencing', 'ChIP', 'CHIPSEQ', FALSE, TRUE, FALSE, FALSE, NOW(),
        NOW())
ON CONFLICT DO NOTHING;

INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'ChIP Seq', 'PAIRED', 'chip_seq_sequencing', 'ChIP', 'CHIPSEQ', FALSE, TRUE, FALSE, FALSE, NOW(),
        NOW())
ON CONFLICT DO NOTHING;

-- rna alignment
INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'RNA', 'SINGLE', 'rna_sequencing', 'RNA', 'RNA', FALSE, FALSE, FALSE, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'RNA', 'PAIRED', 'rna_sequencing', 'RNA', 'RNA', FALSE, FALSE, FALSE, FALSE, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- cell ranger
INSERT INTO seq_type(id, version, name, library_layout, dir_name, display_name, roddy_name, single_cell, has_antibody_target, legacy, needs_bed_file,
                     date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, '10x_scRNA', 'PAIRED', '10x_scRNA_sequencing', '10x_scRNA', NULL, TRUE, FALSE, FALSE, FALSE, NOW(),
        NOW())
ON CONFLICT DO NOTHING;
