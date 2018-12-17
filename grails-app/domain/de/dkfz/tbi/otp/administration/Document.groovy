package de.dkfz.tbi.otp.administration

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.utils.Entity

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

        final String mimeType
        final String extension

        String getDisplayName() {
            "${name()} (.${extension})"
        }
    }

    FormatType formatType
    DocumentType documentType
    byte[] content

    @SuppressWarnings('DuplicateNumberLiteral')
    static constraints = {
        // limit file size to 1MB
        content maxSize: 1024 * 1024, blank: false, validator: {
            if (it.size() == 0) {
                return "the file must not be empty"
            }
        }
    }

    static mapping = {
        documentType index: "document__document_type_idx"
    }

    static belongsTo = [
            documentType: DocumentType,
    ]

    String getFileNameWithExtension() {
        return "${documentType.title}.${formatType.extension}".toLowerCase()
    }
}
