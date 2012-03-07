package de.dkfz.tbi.otp.ngsdata

class AlignmentParams {

    SoftwareTool pipeline
    String genome
    String params

    static constraints = {
        pipeline()
        genome(nullable: true)
        params(nullable: true)
    }

    String toString() {
        pipeline.toString()
    }
}
