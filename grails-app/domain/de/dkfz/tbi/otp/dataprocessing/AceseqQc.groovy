package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.utils.*

class AceseqQc implements Entity{

    AceseqInstance aceseqInstance

    int number

    double normalContamination

    String ploidyFactor

    double ploidy

    double goodnessOfFit

    String purity

    String gender

    int solutionPossible

   static belongsTo = [aceseqInstance: AceseqInstance]
}
