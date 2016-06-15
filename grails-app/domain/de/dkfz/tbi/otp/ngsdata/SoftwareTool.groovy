package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class SoftwareTool implements Entity {

    String programName
    String programVersion
    /** @deprecated <code>null</code> for all instances */ @Deprecated
    String qualityCode
    enum Type {BASECALLING, ALIGNMENT}
    Type type

    static constraints = {
        programName()
        programVersion(nullable: true)
        qualityCode(nullable: true)
        type()
    }

    String getDisplayName() {
        return "${programName} ${programVersion}"
    }

    String toString() {
        return getDisplayName()
    }
}
