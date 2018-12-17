package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdEvaluated
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightValue
import de.dkfz.tbi.otp.utils.Entity

class AceseqQc implements Entity, QcTrafficLightValue {

    AceseqInstance aceseqInstance

    @QcThresholdEvaluated
    int number

    //tumor cell content
    @QcThresholdEvaluated
    double tcc

    String ploidyFactor

    @QcThresholdEvaluated
    double ploidy

    @QcThresholdEvaluated
    double goodnessOfFit

    String gender

    @QcThresholdEvaluated
    int solutionPossible

    static belongsTo = [aceseqInstance: AceseqInstance]

    static constraints = {
        number unique: 'aceseqInstance'
    }
}
