package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*


public class AnalysisDeletionService {
    /**
     * Delete all SnvJobResults, subclasses of BamFilePairAnalysis (such as SnvCallingInstance, IndelCallingInstance,
     * etc), and empty SamplePairs (SamplePairs with no further subclasses of BamFilePairAnalysis) for the given
     * bam file from the database. It is the responsibility of the caller to delete the returned directories.
     * It returns first the directories of the subclasses of BamFilePairAnalysis and then the directories of the deleted
     * SamplePairs for separate deletion by the caller. The SamplePair directories are parent directories of the
     * subclasses of BamFilePairAnalysis directories, therefore the order is important.
     */
    List<File> deleteForAbstractMergedBamFile(AbstractMergedBamFile bamFile) {
        assert bamFile
        List<File> directoriesToDelete = []

        List<BamFilePairAnalysis> analysisInstances = BamFilePairAnalysis.findAllBySampleType1BamFileOrSampleType2BamFile(bamFile, bamFile)
        assertThatNoWorkflowsAreRunning(analysisInstances)
        List<SamplePair> samplePairs = analysisInstances*.samplePair.unique()

        analysisInstances.each {
            directoriesToDelete << deleteInstance(it)
        }

        directoriesToDelete.addAll(deleteSamplePairsWithoutAnalysisInstances(samplePairs))

        return directoriesToDelete
    }


    private static File deleteInstance(BamFilePairAnalysis analysisInstance) {
        File directory = analysisInstance.getInstancePath().getAbsoluteDataManagementPath()
        if (analysisInstance instanceof SnvCallingInstance) {
            List<SnvJobResult> results = SnvJobResult.findAllBySnvCallingInstance(analysisInstance, [sort: 'id', order: 'desc'])
            results.each {
                it.delete(flush: true)
            }
        }
        analysisInstance.delete(flush: true)
        return directory
    }

    static List<File> deleteSamplePairsWithoutAnalysisInstances(List<SamplePair> samplePairs) {
        List<File> directoriesToDelete = []
        samplePairs.each { SamplePair samplePair ->
            if (!SnvCallingInstance.findBySamplePair(samplePair)) {
                directoriesToDelete << samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath()
            }
            if (!IndelCallingInstance.findBySamplePair(samplePair)) {
                directoriesToDelete << samplePair.getIndelSamplePairPath().getAbsoluteDataManagementPath()
            }
            if (!BamFilePairAnalysis.findBySamplePair(samplePair)) {
                samplePair.delete(flush: true)
            }
        }
        return directoriesToDelete
    }

    private static assertThatNoWorkflowsAreRunning(List<BamFilePairAnalysis> instances) {
        if (instances.find {
            it.processingState == AnalysisProcessingStates.IN_PROGRESS && !it.withdrawn
        }) {
            throw new RuntimeException("There are some analysis workflows running for ${instances[0].sampleType1BamFile}")
        }
    }
}
