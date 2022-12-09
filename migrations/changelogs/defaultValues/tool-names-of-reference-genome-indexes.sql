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

-- RNA
INSERT INTO tool_name(id, version, name, path, type, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'ARRIBA_BLACKLIST', 'ariba_blacklist', 'RNA', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO tool_name(id, version, name, path, type, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'ARRIBA_KNOWN_FUSIONS', 'ariba_known_fusions', 'RNA', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO tool_name(id, version, name, path, type, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'GENOME_GATK_INDEX', 'gatk', 'RNA', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO tool_name(id, version, name, path, type, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'GENOME_KALLISTO_INDEX', 'kallisto', 'RNA', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO tool_name(id, version, name, path, type, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'GENOME_STAR_INDEX_100', 'star_100', 'RNA', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO tool_name(id, version, name, path, type, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'GENOME_STAR_INDEX_200', 'star_200', 'RNA', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO tool_name(id, version, name, path, type, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'GENOME_STAR_INDEX_50', 'star_50', 'RNA', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- SINGLE_CELL
INSERT INTO tool_name(id, version, name, path, type, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'CELL_RANGER', 'cellranger', 'SINGLE_CELL', NOW(), NOW())
ON CONFLICT DO NOTHING;
