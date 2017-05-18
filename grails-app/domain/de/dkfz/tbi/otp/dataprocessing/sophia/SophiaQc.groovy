package de.dkfz.tbi.otp.dataprocessing.sophia

import de.dkfz.tbi.otp.utils.*

class SophiaQc implements Entity {

    SophiaInstance sophiaInstance

    int controlMassiveInvPrefilteringLevel
    int tumorMassiveInvFiteringLevel
    String rnaContaminatedGenesMoreThanTwoIntron
    int rnaContaminatedGenesCount
    boolean rnaDecontaminationApplied

    static belongsTo = [sophiaInstance: SophiaInstance]
}
