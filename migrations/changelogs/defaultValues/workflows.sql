/*
 * Copyright 2011-2022 The OTP authors
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

INSERT INTO workflow(id, version, name, bean_name, enabled, priority, date_created, last_updated, max_parallel_workflows)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'FASTQ installation', 'dataInstallationWorkflow', TRUE, 0, NOW(), NOW(), 10)
ON CONFLICT DO NOTHING;

INSERT INTO workflow(id, version, name, enabled, priority, date_created, last_updated, max_parallel_workflows)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'Externally merged BAM files installation', TRUE, 0, NOW(), NOW(), 10)
ON CONFLICT DO NOTHING;


INSERT INTO workflow(id, version, name, bean_name, enabled, priority, date_created, last_updated, max_parallel_workflows)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'Bash FastQC', 'bashFastQcWorkflow', TRUE, 0, NOW(), NOW(), 10)
ON CONFLICT DO NOTHING;


INSERT INTO workflow(id, version, name, bean_name, enabled, priority, date_created, last_updated, max_parallel_workflows)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'PanCancer alignment', 'panCancerWorkflow', TRUE, 0, NOW(), NOW(), 10)
ON CONFLICT DO NOTHING;

INSERT INTO workflow(id, version, name, bean_name, enabled, priority, date_created, last_updated, max_parallel_workflows)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'RNA alignment', 'rnaAlignmentWorkflow', TRUE, 0, NOW(), NOW(), 10)
ON CONFLICT DO NOTHING;

INSERT INTO workflow(id, version, name, bean_name, enabled, priority, date_created, last_updated, max_parallel_workflows)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'WGBS alignment', 'wgbsWorkflow', TRUE, 0, NOW(), NOW(), 10)
ON CONFLICT DO NOTHING;

INSERT INTO workflow(id, version, name, enabled, priority, date_created, last_updated, max_parallel_workflows)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'Cell Ranger', TRUE, 0, NOW(), NOW(), 10)
ON CONFLICT DO NOTHING;


INSERT INTO workflow(id, version, name, enabled, priority, date_created, last_updated, max_parallel_workflows)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'Roddy ACEseq (CNV calling)', TRUE, 0, NOW(), NOW(), 10)
ON CONFLICT DO NOTHING;

INSERT INTO workflow(id, version, name, enabled, priority, date_created, last_updated, max_parallel_workflows)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'Roddy Indel calling', TRUE, 0, NOW(), NOW(), 10)
ON CONFLICT DO NOTHING;

INSERT INTO workflow(id, version, name, enabled, priority, date_created, last_updated, max_parallel_workflows)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'Roddy SNV calling', TRUE, 0, NOW(), NOW(), 10)
ON CONFLICT DO NOTHING;

INSERT INTO workflow(id, version, name, enabled, priority, date_created, last_updated, max_parallel_workflows)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'Roddy Sophia (structural variation calling)', TRUE, 0, NOW(), NOW(), 10)
ON CONFLICT DO NOTHING;

INSERT INTO workflow(id, version, name, enabled, priority, date_created, last_updated, max_parallel_workflows)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'runYapsa (mutational signature analysis)', TRUE, 0, NOW(), NOW(), 10)
ON CONFLICT DO NOTHING;

INSERT INTO workflow(id, version, name, bean_name, enabled, priority, date_created, last_updated, max_parallel_workflows)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'nf-seq-qc', 'wesFastQcWorkflow', TRUE, 0, NOW(), NOW(), 10)
ON CONFLICT DO NOTHING;
