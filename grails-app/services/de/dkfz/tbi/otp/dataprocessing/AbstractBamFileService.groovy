package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*

class AbstractBamFileService {

    /**
     * @param bamFile, which was assigned to a {@link MergingSet}
     */
    void assignedToMergingSet(AbstractBamFile bamFile) {
        notNull(bamFile, "the input bam file for the method assignedToMergingSet is null")
        bamFile.status = State.PROCESSED
        assertSave(bamFile)
    }

    public List<AbstractBamFile> findByMergingSet(MergingSet mergingSet) {
        notNull(mergingSet, "The parameter merging set is not allowed to be null")
        return MergingSetAssignment.findAllByMergingSet(mergingSet)*.bamFile
    }

    public List<AbstractBamFile> findByProcessedMergedBamFile(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "The parameter processedMergedBamFile is not allowed to be null")
        return findByMergingSet(processedMergedBamFile.mergingPass.mergingSet)
    }

    /**
     * returns a list of all single lane bam files, which are merged in several step to the final processedMergedBamFile.
     * It is assumed that only new lanes are merged with the old mergedBamFile to a new one.
     */
    public List<ProcessedBamFile> findAllByProcessedMergedBamFile(ProcessedMergedBamFile processedMergedBamFile) {
        notNull(processedMergedBamFile, "The parameter processedMergedBamFile is not allowed to be null")
        List<AbstractBamFile> results = [processedMergedBamFile]
        while (results.find { it instanceof ProcessedMergedBamFile }) {
            ProcessedMergedBamFile tempFile = results.find { it instanceof ProcessedMergedBamFile }
            results = results - tempFile
            results.addAll(findByProcessedMergedBamFile(tempFile))
        }
        return results
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }
}
