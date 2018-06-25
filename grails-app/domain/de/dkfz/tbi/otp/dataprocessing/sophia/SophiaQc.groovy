package de.dkfz.tbi.otp.dataprocessing.sophia

import de.dkfz.tbi.otp.qcTrafficLight.*
import de.dkfz.tbi.otp.utils.*

class SophiaQc implements Entity, QcTrafficLightValue {

    SophiaInstance sophiaInstance

    @QcThresholdEvaluated
    int controlMassiveInvPrefilteringLevel
    @QcThresholdEvaluated
    int tumorMassiveInvFilteringLevel
    String rnaContaminatedGenesMoreThanTwoIntron
    @QcThresholdEvaluated
    int rnaContaminatedGenesCount
    boolean rnaDecontaminationApplied

    static belongsTo = [sophiaInstance: SophiaInstance]

    static constraints = {
        sophiaInstance unique: true
    }
}
