package de.dkfz.tbi.otp.monitor

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair

class VariantCallingPipelinesChecker extends PipelinesChecker<AbstractMergedBamFile> {

    @Override
    List handle(List<AbstractMergedBamFile> bamFiles, MonitorOutputCollector output) {
        if (!bamFiles) {
            return []
        }

        // 'tier 1' workflows, directly on bams
        List<SamplePair> samplePairs = new SamplePairChecker().handle(bamFiles, output)
        List<BamFilePairAnalysis> snvFinished = new SnvCallingPipelineChecker().handle(samplePairs, output)
        List<BamFilePairAnalysis> indelFinished = new IndelCallingPipelineChecker().handle(samplePairs, output)
        List<BamFilePairAnalysis> sophiaFinished = new SophiaCallingPipelineChecker().handle(samplePairs, output)

        // 'tier 2' workflows, requiring tier-1 results.
        List<BamFilePairAnalysis> aceseqFinished = new AceseqCallingPipelineChecker().handle(sophiaFinished*.samplePair, output)
        List<BamFilePairAnalysis> runYapsaFinished = new RunYapsaPipelineChecker().handle(snvFinished*.samplePair, output)

        // samplepairs that are COMPLETELY done.
        // CAVEAT: if one of these workflows is not configured to run at all (e.g. disabled for project)
        // this will never return anything.
        List<SamplePair> finished = samplePairs.intersect(snvFinished*.samplePair)
                .intersect(indelFinished*.samplePair)
                .intersect(sophiaFinished*.samplePair)
                .intersect(aceseqFinished*.samplePair)
                .intersect(runYapsaFinished*.samplePair)
        return finished
    }
}
