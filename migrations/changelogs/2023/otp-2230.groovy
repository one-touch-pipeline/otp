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
    changeSet(author: "Julian Rausch", id: "1702560679049-88") {
        dropView(viewName: "sequences", ifExists: "true")
    }

    changeSet(author: "Julian Rausch", id: "1702560679049-89") {
        addColumn(tableName: "project") {
            column(name: "state", type: "varchar(255)", defaultValue: "OPEN") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "Julian Rausch", id: "1702560679049-125") {
        update(tableName: "project") {
            column(name: "state", valueComputed: "CASE WHEN closed = true THEN 'CLOSED' ELSE state END")
        }
    }

    changeSet(author: "Julian Rausch", id: "1702560679049-124") {
        update(tableName: "project") {
            column(name: "state", valueComputed: "CASE WHEN archived = true THEN 'ARCHIVED' ELSE state END")
        }
    }

    changeSet(author: "Julian Rausch", id: "1702560679049-126") {
        dropColumn(columnName: "archived", tableName: "project")
    }

    changeSet(author: "Julian Rausch", id: "1702560679049-127") {
        dropColumn(columnName: "closed", tableName: "project")
    }

    changeSet(author: "Julian Rausch", id: "1702565243412-41") {
        dropDefaultValue(columnDataType: "varchar(255)", columnName: "state", tableName: "project")
    }
}
