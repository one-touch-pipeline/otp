package de.dkfz.tbi.otp.ngsdata

class SoftwareTool {

    String programName
    String programVersion
    String qualityCode
    enum Type {BASECALLING, ALIGNMENT}
    Type type

    static constraints = {
        programName()
        programVersion(nullable: true)
        qualityCode(nullable: true)
        type()
    }

    String toString() {
        return "${programName} ${programVersion}"
    }
}
