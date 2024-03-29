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

-- fastqc versions
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'Bash FastQC') AND wav.identifier = 1),
        '0.11.5', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- nf-seq-qc
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'nf-seq-qc') AND wav.identifier = 1),
        '1.1.0', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- pancancer versions
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND wav.identifier = 1),
        '1.2.182', NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND wav.identifier = 1),
        '1.2.51-1', NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND wav.identifier = 1),
        '1.2.51-2', NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND wav.identifier = 1),
        '1.2.73-1', NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND wav.identifier = 1),
        '1.2.73-201', NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND wav.identifier = 1),
        '1.2.73-202', NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'PanCancer alignment') AND wav.identifier = 1),
        '1.2.73-204', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- wgbs pancancer versions
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND wav.identifier = 1),
        '1.2.51-1', NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND wav.identifier = 1),
        '1.2.73-1', NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND wav.identifier = 1),
        '1.2.73-2', NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND wav.identifier = 1),
        '1.2.73-201', NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND wav.identifier = 1),
        '1.2.73-202', NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'WGBS alignment') AND wav.identifier = 1),
        '1.2.73-204', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- rna versions
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'RNA alignment') AND wav.identifier = 1),
        '1.2.22-6', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'RNA alignment') AND wav.identifier = 1),
        '1.2.22-7', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'RNA alignment') AND wav.identifier = 1),
        '1.3.0', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'RNA alignment') AND wav.identifier = 1),
        '1.3.0-1', NOW(), NOW())
ON CONFLICT DO NOTHING;

--cell ranger versions
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'Cell Ranger') AND wav.identifier = 1),
        'cellranger/7.1.0', NOW(),
        NOW())
ON CONFLICT DO NOTHING;

--SNV versions
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy SNV calling') AND wav.identifier = 1),
        '1.2.166-3', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy SNV calling') AND wav.identifier = 1),
        '1.2.166-5', NOW(), NOW())
ON CONFLICT DO NOTHING;

--Indel versions
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Indel calling') AND wav.identifier = 1),
        '2.4.1', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Indel calling') AND wav.identifier = 1),
        '2.4.1-1', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id FROM workflow_api_version wav WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Indel calling') AND wav.identifier = 1),
        '2.4.1-2', NOW(), NOW())
ON CONFLICT DO NOTHING;

--sophia versions
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id
         FROM workflow_api_version wav
         WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy Sophia (structural variation calling)')
           AND wav.identifier = 1),
        '2.2.3', NOW(), NOW())
ON CONFLICT DO NOTHING;

--aceseq versions
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id
         FROM workflow_api_version wav
         WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'Roddy ACEseq (CNV calling)')
           AND wav.identifier = 1),
        '1.2.8-4', NOW(), NOW())
ON CONFLICT DO NOTHING;

--runYapsa versions
INSERT INTO workflow_version(id, version, api_version_id, workflow_version, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0,
        (SELECT id
         FROM workflow_api_version wav
         WHERE wav.workflow_id = (SELECT id FROM workflow WHERE name = 'runYapsa (mutational signature analysis)')
           AND wav.identifier = 1),
        'yapsa-devel/b765fa8', NOW(), NOW())
ON CONFLICT DO NOTHING;
