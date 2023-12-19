/*
 * Copyright 2011-2023 The OTP authors
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


--bash fastqc
INSERT INTO workflow_api_version(id, date_created, last_updated, version, workflow_id, identifier)
VALUES (NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, (SELECT id FROM workflow WHERE name = 'Bash FastQC'), 1)
ON CONFLICT DO NOTHING;

--nf-seq-qc
INSERT INTO workflow_api_version(id, date_created, last_updated, version, workflow_id, identifier)
VALUES (NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, (SELECT id FROM workflow WHERE name = 'nf-seq-qc'), 1)
ON CONFLICT DO NOTHING;

--PanCancer alignment
INSERT INTO workflow_api_version(id, date_created, last_updated, version, workflow_id, identifier)
VALUES (NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, (SELECT id FROM workflow WHERE name = 'PanCancer alignment'), 1)
ON CONFLICT DO NOTHING;

--RNA Alignment
INSERT INTO workflow_api_version(id, date_created, last_updated, version, workflow_id, identifier)
VALUES (NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, (SELECT id FROM workflow WHERE name = 'RNA alignment'), 1)
ON CONFLICT DO NOTHING;

--WGBS alignment
INSERT INTO workflow_api_version(id, date_created, last_updated, version, workflow_id, identifier)
VALUES (NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, (SELECT id FROM workflow WHERE name = 'WGBS alignment'), 1)
ON CONFLICT DO NOTHING;

--Cell Ranger
INSERT INTO workflow_api_version(id, date_created, last_updated, version, workflow_id, identifier)
VALUES (NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, (SELECT id FROM workflow WHERE name = 'Cell Ranger'), 1)
ON CONFLICT DO NOTHING;

--Roddy SNV calling
INSERT INTO workflow_api_version(id, date_created, last_updated, version, workflow_id, identifier)
VALUES (NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, (SELECT id FROM workflow WHERE name = 'Roddy SNV calling'), 1)
ON CONFLICT DO NOTHING;

--Roddy Indel calling
INSERT INTO workflow_api_version(id, date_created, last_updated, version, workflow_id, identifier)
VALUES (NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, (SELECT id FROM workflow WHERE name = 'Roddy Indel calling'), 1)
ON CONFLICT DO NOTHING;

--Roddy Sophia (structural variation calling)
INSERT INTO workflow_api_version(id, date_created, last_updated, version, workflow_id, identifier)
VALUES (NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, (SELECT id FROM workflow WHERE name = 'Roddy Sophia (structural variation calling)'), 1)
ON CONFLICT DO NOTHING;

--Roddy ACEseq (CNV calling)
INSERT INTO workflow_api_version(id, date_created, last_updated, version, workflow_id, identifier)
VALUES (NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, (SELECT id FROM workflow WHERE name = 'Roddy ACEseq (CNV calling)'), 1)
ON CONFLICT DO NOTHING;

--runYapsa (mutational signature analysis)
INSERT INTO workflow_api_version(id, date_created, last_updated, version, workflow_id, identifier)
VALUES (NEXTVAL('hibernate_sequence'), NOW(), NOW(), 0, (SELECT id FROM workflow WHERE name = 'runYapsa (mutational signature analysis)'), 1)
ON CONFLICT DO NOTHING;
