package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*

public class AnalysisDeletionService {

    /**
     * Delete all SnvJobResults, subclasses of BamFilePairAnalysis (such as SnvCallingInstance, IndelCallingInstance, etc) from the database.
     */
    static File deleteInstance(BamFilePairAnalysis analysisInstance) {
        File directory = analysisInstance.getInstancePath().getAbsoluteDataManagementPath()
        if (analysisInstance instanceof SnvCallingInstance) {
            List<SnvJobResult> results = SnvJobResult.findAllBySnvCallingInstance(analysisInstance, [sort: 'id', order: 'desc'])
            results.each {
                it.delete(flush: true)
            }
        } else if (analysisInstance instanceof IndelCallingInstance) {
            List<IndelQualityControl> indelQualityControl = IndelQualityControl.findAllByIndelCallingInstance(analysisInstance, [sort: 'id', order: 'desc'])
            indelQualityControl.each {
                it.delete(flush: true)
            }
            List<IndelSampleSwapDetection> indelSampleSwapDetections = IndelSampleSwapDetection.findAllByIndelCallingInstance(analysisInstance, [sort: 'id', order: 'desc'])
            indelSampleSwapDetections.each {
                it.delete(flush: true)
            }
        } else if (analysisInstance instanceof SophiaInstance) {
            List<SophiaQc> sophiaQc = SophiaQc.findAllBySophiaInstance(analysisInstance, [sort: 'id', order: 'desc'])
            sophiaQc.each {
                it.delete(flush: true)
            }
        } else if (analysisInstance instanceof AceseqInstance) {
            List<AceseqQc> aceseqQc = AceseqQc.findAllByAceseqInstance(analysisInstance, [sort: 'id', order: 'desc'])
            aceseqQc.each {
                it.delete(flush: true)
            }
        }
        analysisInstance.delete(flush: true)
        return directory
    }

    /**
     * Delete empty SamplePairs (SamplePairs with no further subclasses of BamFilePairAnalysis).
     * The SamplePair directories are parent directories of the subclasses of BamFilePairAnalysis directories,
     * therefore this method has to run after deleteInstance().
     */
    static List<File> deleteSamplePairsWithoutAnalysisInstances(List<SamplePair> samplePairs) {
        List<File> directoriesToDelete = []
        samplePairs.unique().each { SamplePair samplePair ->
            if (!SnvCallingInstance.findBySamplePair(samplePair)) {
                directoriesToDelete << samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath()
            }
            if (!IndelCallingInstance.findBySamplePair(samplePair)) {
                directoriesToDelete << samplePair.getIndelSamplePairPath().getAbsoluteDataManagementPath()
            }
            if (!AceseqInstance.findBySamplePair(samplePair)) {
                directoriesToDelete << samplePair.getAceseqSamplePairPath().getAbsoluteDataManagementPath()
            }
            if (!SophiaInstance.findBySamplePair(samplePair)) {
                directoriesToDelete << samplePair.getSophiaSamplePairPath().getAbsoluteDataManagementPath()
            }
            if (!BamFilePairAnalysis.findBySamplePair(samplePair)) {
                samplePair.delete(flush: true)
            }
        }
        return directoriesToDelete
    }

    static void assertThatNoWorkflowsAreRunning(List<BamFilePairAnalysis> instances) {
        if (instances.find {
            it.processingState == AnalysisProcessingStates.IN_PROGRESS && !it.withdrawn
        }) {
            throw new RuntimeException("There are some analysis workflows running for ${instances[0].sampleType1BamFile}")
        }
    }
}
