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

-- Strains
INSERT INTO strain(id, version, name, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'No strain available', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO strain(id, version, name, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'Unknown', NOW(), NOW())
ON CONFLICT DO NOTHING;


-- Human
INSERT INTO species_common_name(id, version, name, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'Human', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO species(id, version, scientific_name, date_created, last_updated, species_common_name_id)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'Homo sapiens', NOW(), NOW(), (SELECT id FROM species_common_name WHERE name = 'Human'))
ON CONFLICT DO NOTHING;

INSERT INTO species_with_strain(id, version, species_id, strain_id, date_created, last_updated, legacy)
VALUES (NEXTVAL('hibernate_sequence'), 0, (SELECT id FROM species WHERE scientific_name = 'Homo sapiens'),
        (SELECT id FROM strain WHERE name = 'No strain available'), NOW(), NOW(), FALSE)
ON CONFLICT DO NOTHING;


-- Mouse
INSERT INTO species_common_name(id, version, name, date_created, last_updated)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'Mouse', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO species(id, version, scientific_name, date_created, last_updated, species_common_name_id)
VALUES (NEXTVAL('hibernate_sequence'), 0, 'Mus musculus', NOW(), NOW(), (SELECT id FROM species_common_name WHERE name = 'Mouse'))
ON CONFLICT DO NOTHING;

INSERT INTO species_with_strain(id, version, species_id, strain_id, date_created, last_updated, legacy)
VALUES (NEXTVAL('hibernate_sequence'), 0, (SELECT id FROM species WHERE scientific_name = 'Mus musculus'), (SELECT id FROM strain WHERE name = 'Unknown'),
        NOW(), NOW(), FALSE)
ON CONFLICT DO NOTHING;
