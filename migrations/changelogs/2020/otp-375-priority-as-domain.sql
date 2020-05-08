/*
 * Copyright 2011-2020 The OTP authors
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

INSERT INTO processing_priority(id, version, date_created, last_updated, name, priority, error_mail_prefix, queue, roddy_config_suffix,
                                allowed_parallel_workflow_runs)
VALUES (nextval('hibernate_sequence'), 0, now(), now(), 'NORMAL', 0, 'ERROR', 'normal', 'normal', 250),
       (nextval('hibernate_sequence'), 0, now(), now(), 'FASTTRACK', 100000, 'FASTTRACK ERROR', 'fasttrack', 'fasttrack', 500),
       (nextval('hibernate_sequence'), 0, now(), now(), 'EXTREME FASTTRACK', 200000, 'FASTTRACK ERROR', 'fasttrack', 'fasttrack', 1000),
       (nextval('hibernate_sequence'), 0, now(), now(), 'MINIMAL', -100000, 'ERROR', 'normal', 'normal', 50),
       (nextval('hibernate_sequence'), 0, now(), now(), 'REPROCESSING', -1000, 'ERROR', 'normal', 'normal', 100);

UPDATE project
SET processing_priority_id = (SELECT id FROM processing_priority WHERE name = 'MINIMAL')
WHERE processing_priority < 0;

UPDATE project
SET processing_priority_id = (SELECT id FROM processing_priority WHERE name = 'NORMAL')
WHERE processing_priority >= 0
  AND processing_priority < 10000; --10000 is the current value for fasttrack

UPDATE project
SET processing_priority_id = (SELECT id FROM processing_priority WHERE name = 'FASTTRACK')
WHERE processing_priority >= 10000;
