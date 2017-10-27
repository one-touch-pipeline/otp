package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

class AceseqQc implements Entity, QcTrafficLightValue {

    AceseqInstance aceseqInstance

    int number

    //tumor cell content
    double tcc

    String ploidyFactor

    double ploidy

    double goodnessOfFit

    String gender

    int solutionPossible

    static belongsTo = [aceseqInstance: AceseqInstance]

    static constraints = {
        number unique: 'aceseqInstance'
    }
}
