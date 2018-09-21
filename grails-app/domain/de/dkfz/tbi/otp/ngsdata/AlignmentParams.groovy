package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

class AlignmentParams implements Entity {

    SoftwareTool pipeline
    String genome
    String params

    static constraints = {
        pipeline()
        genome(nullable: true)
        params(nullable: true)
    }

    @Override
    String toString() {
        pipeline.toString()
    }
}
