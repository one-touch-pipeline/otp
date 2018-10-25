package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.utils.Entity

class DocumentType implements Entity{

    String title
    String description

    static mapping = {
        title index : "document_type_title_idx"
        description type : "text"
    }

    static constraints = {
        title blank : false, unique : true
        description blank : false
    }
}
