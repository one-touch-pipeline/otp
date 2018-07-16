package de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.ngsdata.*

trait RoddyJobSpec implements StartJobIntegrationSpec {

    @Override
    ConfigPerProject createConfig(SamplePair samplePair, Pipeline pipeline) {
        return DomainFactory.createRoddyWorkflowConfig(
                [
                        project : samplePair.project,
                        seqType : samplePair.seqType,
                        pipeline: pipeline,
                ]
        )
    }
}
