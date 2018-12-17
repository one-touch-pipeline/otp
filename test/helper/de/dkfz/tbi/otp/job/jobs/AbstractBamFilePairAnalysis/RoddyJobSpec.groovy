package de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis

import de.dkfz.tbi.otp.dataprocessing.ConfigPerProjectAndSeqType
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.DomainFactory

trait RoddyJobSpec implements StartJobIntegrationSpec {

    @Override
    ConfigPerProjectAndSeqType createConfig(SamplePair samplePair, Pipeline pipeline) {
        return DomainFactory.createRoddyWorkflowConfig(
                [
                        project : samplePair.project,
                        seqType : samplePair.seqType,
                        pipeline: pipeline,
                ]
        )
    }
}
