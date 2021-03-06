/*
 * Copyright 2011-2021 The OTP authors
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

INSERT INTO workflow_version(id, version, workflow_id, workflow_version, date_created, last_updated)
VALUES (nextval('hibernate_sequence'), 0, (SELECT id FROM workflow WHERE name = 'PanCancer alignment'), '1.2.73-201', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, workflow_id, workflow_version, date_created, last_updated)
VALUES (nextval('hibernate_sequence'), 0, (SELECT id FROM workflow WHERE name = 'WGBS alignment'), '1.2.73-201', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, workflow_id, workflow_version, date_created, last_updated)
VALUES (nextval('hibernate_sequence'), 0, (SELECT id FROM workflow WHERE name = 'RNA alignment'), '1.3.0-1', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, workflow_id, workflow_version, date_created, last_updated)
VALUES (nextval('hibernate_sequence'), 0, (SELECT id FROM workflow WHERE name = 'Cell Ranger'), 'cellranger/4.0.0', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, workflow_id, workflow_version, date_created, last_updated)
VALUES (nextval('hibernate_sequence'), 0, (SELECT id FROM workflow WHERE name = 'Roddy SNV calling'), '1.2.166-3', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, workflow_id, workflow_version, date_created, last_updated)
VALUES (nextval('hibernate_sequence'), 0, (SELECT id FROM workflow WHERE name = 'Roddy Indel calling'), '2.4.1', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, workflow_id, workflow_version, date_created, last_updated)
VALUES (nextval('hibernate_sequence'), 0, (SELECT id FROM workflow WHERE name = 'Roddy Sophia (structural variation calling)'), '2.2.3', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, workflow_id, workflow_version, date_created, last_updated)
VALUES (nextval('hibernate_sequence'), 0, (SELECT id FROM workflow WHERE name = 'Roddy ACEseq (CNV calling)'), '1.2.8-4', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO workflow_version(id, version, workflow_id, workflow_version, date_created, last_updated)
VALUES (nextval('hibernate_sequence'), 0, (SELECT id FROM workflow WHERE name = 'runYapsa (mutational signature analysis)'),
        'yapsa-devel/b765fa8', now(), now())
ON CONFLICT DO NOTHING;
