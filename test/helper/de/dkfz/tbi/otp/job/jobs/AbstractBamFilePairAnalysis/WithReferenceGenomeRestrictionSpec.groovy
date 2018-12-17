package de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.DomainFactory

trait WithReferenceGenomeRestrictionSpec implements StartJobIntegrationSpec {

    @Override
    SamplePair setupSamplePair() {
        SamplePair samplePair = super.setupSamplePair()

        DomainFactory.createProcessingOptionLazy([
                name: getProcessingOptionNameForReferenceGenome(),
                type: null,
                project: null,
                value: samplePair.mergingWorkPackage1.referenceGenome.name,
        ])

        return samplePair
    }

    abstract ProcessingOption.OptionName getProcessingOptionNameForReferenceGenome()
}
