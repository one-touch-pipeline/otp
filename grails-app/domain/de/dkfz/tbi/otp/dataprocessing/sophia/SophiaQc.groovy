package de.dkfz.tbi.otp.dataprocessing.sophia

import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdEvaluated
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightValue
import de.dkfz.tbi.otp.utils.Entity

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
