package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*

class VariantCallingPipelinesChecker extends PipelinesChecker<AbstractMergedBamFile> {

    List handle(List<AbstractMergedBamFile> bamFiles, MonitorOutputCollector output) {
        if (!bamFiles) {
            return []
        }
        List<SamplePair> samplePairs = new SamplePairChecker().handle(bamFiles, output)
        List<BamFilePairAnalysis> snvFinished = new SnvCallingPipelineChecker().handle(samplePairs, output)
        List<BamFilePairAnalysis> indelFinished = new IndelCallingPipelineChecker().handle(samplePairs, output)
        List<BamFilePairAnalysis> aceseqFinished = new AceseqCallingPipelineChecker().handle(samplePairs, output)
        List<SamplePair> finished = samplePairs.intersect(snvFinished*.samplePair).
                intersect(indelFinished*.samplePair).
                intersect(aceseqFinished*.samplePair)
        return finished
    }
}
