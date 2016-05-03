package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.ReferencedClass

class ChangeLog implements Entity {

    long rowId
    /**
     * The class this ChangeLog is pointing to
     */
    ReferencedClass referencedClass
    /**
     * This field has been dropped, only listed for backward compatibility of database
     * @deprecated Use referencedClass
     */
    String tableName = ""
    String columnName
    String fromValue
    String toValue
    String comment
    Date dateCreated = new Date()

    enum Source {
        SYSTEM, MANUAL
    }
    Source source
    static constraints = {
        // blank to be able to keep existing objects in Database
        tableName(blank: true)
        // nullable for compatibility with existing objects in Database
        referencedClass(nullable: true)
        columnName()
        fromValue()
        toValue()
        comment()
        // nullable for compatibility with existing objects in Database
        dateCreated(nullable: true)
    }
}
