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

databaseChangeLog = {
    changeSet(author: "borufka (generated)", id: "otp-442-1") {
        addColumn(tableName: "import_process") {
            column(name: "link_operation", type: "varchar(255)")
        }
    }

    changeSet(author: "borufka", id: "otp-442-2") {
        sql("""UPDATE import_process SET link_operation = 'COPY_AND_LINK' WHERE replace_source_with_link = true;
               UPDATE import_process SET link_operation = 'COPY_AND_KEEP' WHERE replace_source_with_link = false;""")
    }

    changeSet(author: "borufka (generated)", id: "otp-442-3") {
        addNotNullConstraint(columnDataType: "varchar(255)", columnName: "link_operation", tableName: "import_process")
    }

    changeSet(author: "borufka (generated)", id: "otp-442-4") {
        dropColumn(columnName: "replace_source_with_link", tableName: "import_process")
    }
}

