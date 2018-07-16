package de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*

trait BamFilePairAnalysisStartJobTrait {

    abstract void prepareCreatingTheProcessAndTriggerTracking(BamFilePairAnalysis bamFilePairAnalysis)

    abstract ConfigPerProject getConfig(SamplePair samplePair)
    abstract BamFileAnalysisService getBamFileAnalysisService()

    abstract String getInstanceName(ConfigPerProject config)
    abstract String getFormattedDate()
}
