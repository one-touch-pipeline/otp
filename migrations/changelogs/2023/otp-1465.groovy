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

databaseChangeLog = {
    changeSet(author: "-", id: "1689851724804-100") {
        sql("""
            ALTER TABLE project_keywords RENAME TO project_keyword;
            ALTER TABLE project_keyword RENAME COLUMN project_id TO project_keywords_id;
            ALTER TABLE project_keyword DROP CONSTRAINT project_keywords_pkey;
            ALTER TABLE project_keyword ALTER COLUMN keyword_id DROP NOT NULL;

            INSERT INTO keyword(id, version, name, date_created, last_updated) SELECT NEXTVAL('hibernate_sequence'), 0, keywords_string, NOW(), NOW()
                                                                               FROM project_request_keywords WHERE NOT EXISTS(SELECT name FROM keyword WHERE name = keywords_string);
            ALTER TABLE project_request_keywords ADD COLUMN keyword_id bigint REFERENCES keyword;
            UPDATE project_request_keywords SET keyword_id = (SELECT id FROM keyword WHERE keywords_string = name);
            ALTER TABLE project_request_keywords DROP COLUMN keywords_string;

            ALTER TABLE project_request_keywords RENAME TO project_request_keyword;
            ALTER TABLE project_request_keyword RENAME COLUMN project_request_id TO project_request_keywords_id;

            CREATE INDEX project_keyword_project_keywords_idx ON project_keyword (project_keywords_id);
            CREATE INDEX project_keyword_keyword_idx ON project_keyword (keyword_id);
        """)
    }
}
