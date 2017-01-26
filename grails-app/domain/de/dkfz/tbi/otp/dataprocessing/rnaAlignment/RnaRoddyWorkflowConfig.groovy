package de.dkfz.tbi.otp.dataprocessing.rnaAlignment

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.ngsdata.*

class RnaRoddyWorkflowConfig extends RoddyWorkflowConfig {

    GeneModel geneModel

    static hasMany = [
            referenceGenomeIndex: ReferenceGenomeIndex,
    ]

    static belongsTo = [
            geneModel: GeneModel
    ]
}