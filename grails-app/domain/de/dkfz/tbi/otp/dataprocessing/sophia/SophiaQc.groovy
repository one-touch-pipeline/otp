package de.dkfz.tbi.otp.dataprocessing.sophia

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class SophiaQc implements Entity, QcTrafficLightValue {

    SophiaInstance sophiaInstance

    int controlMassiveInvPrefilteringLevel
    int tumorMassiveInvFilteringLevel
    String rnaContaminatedGenesMoreThanTwoIntron
    int rnaContaminatedGenesCount
    boolean rnaDecontaminationApplied

    static belongsTo = [sophiaInstance: SophiaInstance]

    static constraints = {
        sophiaInstance unique: true
    }
}
