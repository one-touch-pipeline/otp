package de.dkfz.tbi.otp.ngsdata

class ChangeLog {

    long rowId
    String tableName
    String columnName
    String fromValue
    String toValue
    String comment

    enum Source {
        SYSTEM, MANUAL
    }
    Source source
    static constraints = {
    }
}
