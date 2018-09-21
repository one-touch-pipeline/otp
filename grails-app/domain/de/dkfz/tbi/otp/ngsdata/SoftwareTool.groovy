package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class SoftwareTool implements Entity {

    String programName
    String programVersion
    enum Type {
        BASECALLING, ALIGNMENT
    }
    Type type

    static constraints = {
        programName()
        programVersion(nullable: true)
        type()
    }

    String getDisplayName() {
        return "${programName} ${programVersion}"
    }

    @Override
    String toString() {
        return getDisplayName()
    }
}
