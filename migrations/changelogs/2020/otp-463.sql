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


CREATE TABLE user_project_role_project_role(
    user_project_role_project_roles_id BIGINT,
    project_role_id BIGINT,
    FOREIGN KEY (user_project_role_project_roles_id) REFERENCES user_project_role(id),
    FOREIGN KEY (project_role_id) REFERENCES project_role(id),
    PRIMARY KEY (user_project_role_project_roles_id, project_role_id)
);

CREATE INDEX user_project_role_project_roles_id_idx ON user_project_role_project_role(user_project_role_project_roles_id);
CREATE INDEX project_role_id_idx ON user_project_role_project_role(project_role_id);

INSERT INTO user_project_role_project_role
SELECT id, project_role_id FROM user_project_role;

