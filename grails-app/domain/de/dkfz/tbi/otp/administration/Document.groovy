/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.administration

import grails.gorm.hibernate.annotation.ManagedEntity
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.utils.Entity

@ManagedEntity
class Document implements Entity {

    @TupleConstructor
    enum FormatType {
        PDF("application/pdf", "pdf"),
        WORD("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
        WORD_97("application/msword", "doc"),
        EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
        EXCEL_97("application/vnd.ms-excel", "xls"),
        CSV("text/csv", "csv"),
        TSV("text/tab-separated-values", "tsv"),
        TXT("text/plain", "txt"),
        LINK("", ""),

        final String mimeType
        final String extension

        String getDisplayName() {
            "${name()} (.${extension})"
        }
    }

    FormatType formatType
    DocumentType documentType
    byte[] content
    String link

    @SuppressWarnings('DuplicateNumberLiteral')
    static constraints = {
        // limit file size to 1MB
        content maxSize: 1024 * 1024, nullable: true, validator: { val, obj ->
            if (obj.formatType != FormatType.LINK && (!val || val.size() == 0)) {
                return "empty"
            }
        }
        link nullable: true, validator: { val, obj ->
            if (obj.formatType == FormatType.LINK && !val) {
                return "empty"
            }
        }
    }

    static mapping = {
        documentType index: "document__document_type_idx"
    }

    static belongsTo = [
            documentType: DocumentType,
    ]
}
