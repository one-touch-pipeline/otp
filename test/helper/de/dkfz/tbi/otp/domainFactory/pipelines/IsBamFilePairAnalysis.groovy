package de.dkfz.tbi.otp.domainFactory.pipelines

import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair

trait IsBamFilePairAnalysis implements IsPipeline {

    abstract BamFilePairAnalysis createBamFilePairAnalysis(SamplePair samplePair, Map properties = [:])

    abstract BamFilePairAnalysis createBamFilePairAnalysisWithRoddyBamFiles(Map properties = [:], Map bamFile1Properties = [:], Map bamFile2Properties = [:])

    BamFilePairAnalysis createBamFilePairAnalysisWithSameSamplePair(BamFilePairAnalysis instance) {
        return createBamFilePairAnalysis(instance.samplePair)
    }
}
