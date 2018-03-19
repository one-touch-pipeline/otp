package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.utils.*
import groovy.transform.*


class Document implements Entity {
    enum Name {
        PROJECT_FORM,
        METADATA_TEMPLATE,
        PROCESSING_INFORMATION,
    }

    @TupleConstructor
    enum Type {
        PDF("application/pdf", "pdf"),
        WORD("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
        WORD_97("application/msword", "doc"),
        EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
        EXCEL_97("application/vnd.ms-excel", "xls"),
        CSV("text/csv", "csv"),
        TSV("text/tab-separated-values", "tsv"),

        final String mimeType
        final String extension

        String getDisplayName() {
            "${name()} (.${extension})"
        }
    }

    Type type
    Name name
    byte[] content

    static constraints = {
        // limit file size to 1MB
        content maxSize: 1024 * 1024, blank: false, validator: {
            if (it.size() == 0) {
                return "the file must not be empty"
            }
        }
    }

    static mapping = {
        name index: "document__name_idx"
    }
}
