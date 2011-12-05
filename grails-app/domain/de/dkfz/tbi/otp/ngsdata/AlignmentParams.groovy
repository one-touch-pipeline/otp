package de.dkfz.tbi.otp.ngsdata

class AlignmentParams {

    String programName
    String programVersion
    String genome
    String params

    static hasMany = [
        alignmentLogs : AlignmentLog,
        mergingLogs : MergingLog
    ]

    static constraints = {
        programName(blank: false)
        programVersion(nullable: true)
        genome(nullable: true)
        params(nullable: true)
    }

    String toString() {
        "${programName} ${programVersion}"
    }
}
