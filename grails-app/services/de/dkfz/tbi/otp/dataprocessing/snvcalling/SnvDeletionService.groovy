package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile

public class SnvDeletionService {


    /**
     * Delete all SnvJobResults, SnvCallingInstances and SamplePairs for the given bamFile.
     * It returns the directories of the samplePairs for separate deletion by the caller.
     */
    List<File> deleteForProcessedMergedBamFile(ProcessedMergedBamFile bamFile) {
        assert bamFile
        List<SnvCallingInstance> instances = SnvCallingInstance.findAllBySampleType1BamFileOrSampleType2BamFile(bamFile, bamFile)
        assertThatNoSnvAreRunning(instances)

        List<SamplePair> samplePairs = instances*.samplePair.unique()
        List<File> paths = pathsForSamplePairs(samplePairs)

        instances.each {
            deleteInstance(it)
        }
        samplePairs*.delete()
        return paths
    }


    private void deleteInstance(SnvCallingInstance snvCallingInstance) {
        List<SnvJobResult> results = SnvJobResult.findAllBySnvCallingInstance(snvCallingInstance, [sort: 'id', order: 'desc'])
        results.each {
            it.delete()
        }
        snvCallingInstance.delete()
    }

    private List<File> pathsForSamplePairs(List<SamplePair> samplePairs) {
        return samplePairs.collect {
            it.getSamplePairPath().getAbsoluteDataManagementPath()
        }
    }

    private assertThatNoSnvAreRunning(List<SnvCallingInstance> instances) {
        if (instances.find {
            it.processingState == SnvProcessingStates.IN_PROGRESS
        }) {
            throw new RuntimeException("There are some snv callings running for ${instances[0].sampleType1BamFile}")
        }

    }


}
