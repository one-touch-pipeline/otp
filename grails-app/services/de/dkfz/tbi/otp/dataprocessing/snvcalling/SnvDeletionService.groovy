package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates

public class SnvDeletionService {


    /**
     * Delete all SnvJobResults, SnvCallingInstances, and empty SamplePairs (SamplePairs with no further SnvCallingInstance)
     * for the given bam file from the database. It is the responsibility of the caller to delete the returned directories.
     * It returns first the directories of the SnvCallingInstances and then the directories of the deleted SamplePairs
     * for separate deletion by the caller. The SamplePair directories are parent directories of the SnvCallingInstance directories,
     * therefore the order is important.
     */
    List<File> deleteForAbstractMergedBamFile(AbstractMergedBamFile bamFile) {
        assert bamFile
        List<File> directoriesToDelete = []

        List<SnvCallingInstance> instances = SnvCallingInstance.findAllBySampleType1BamFileOrSampleType2BamFile(bamFile, bamFile)
        assertThatNoSnvAreRunning(instances)
        List<SamplePair> samplePairs = instances*.samplePair.unique()

        instances.each {
            directoriesToDelete << deleteInstance(it)
        }

        directoriesToDelete.addAll(deleteSamplePairsWithoutSnvCallingInstances(samplePairs))

        return directoriesToDelete
    }


    private File deleteInstance(SnvCallingInstance snvCallingInstance) {
        File directory = snvCallingInstance.getSnvInstancePath().getAbsoluteDataManagementPath()
        List<SnvJobResult> results = SnvJobResult.findAllBySnvCallingInstance(snvCallingInstance, [sort: 'id', order: 'desc'])
        results.each {
            it.delete(flush: true)
        }
        snvCallingInstance.delete(flush: true)
        return directory
    }

    private List<File> deleteSamplePairsWithoutSnvCallingInstances(List<SamplePair> samplePairs) {
        List<File> directoriesToDelete = []
        samplePairs.each { SamplePair samplePair ->
            if (!SnvCallingInstance.findBySamplePair(samplePair)) {
                directoriesToDelete << samplePair.getSnvSamplePairPath().getAbsoluteDataManagementPath()
                samplePair.delete(flush: true)
            }
        }
        return directoriesToDelete
    }

    private assertThatNoSnvAreRunning(List<SnvCallingInstance> instances) {
        if (instances.find {
            it.processingState == AnalysisProcessingStates.IN_PROGRESS && !it.withdrawn
        }) {
            throw new RuntimeException("There are some snv callings running for ${instances[0].sampleType1BamFile}")
        }

    }


}
