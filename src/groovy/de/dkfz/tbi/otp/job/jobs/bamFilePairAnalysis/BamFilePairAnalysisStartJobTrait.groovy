package de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair

trait BamFilePairAnalysisStartJobTrait {

    abstract void prepareCreatingTheProcessAndTriggerTracking(BamFilePairAnalysis bamFilePairAnalysis)

    abstract ConfigPerProjectAndSeqType getConfig(SamplePair samplePair)
    abstract BamFileAnalysisService getBamFileAnalysisService()

    abstract String getInstanceName(ConfigPerProjectAndSeqType config)
    abstract String getFormattedDate()
}
