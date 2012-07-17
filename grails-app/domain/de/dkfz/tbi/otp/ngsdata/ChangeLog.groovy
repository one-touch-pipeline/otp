package de.dkfz.tbi.otp.ngsdata

class ChangeLog {

    long rowId
    String tableName
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
        tableName()
        columnName()
        fromValue()
        toValue()
        comment()
        // nullable for compatibility with existing objects in Database
        dateCreated(nullable: true)
    }
}
