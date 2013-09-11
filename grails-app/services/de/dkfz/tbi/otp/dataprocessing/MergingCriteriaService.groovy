package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage.MergingCriteria
import de.dkfz.tbi.otp.ngsdata.*


class MergingCriteriaService {

    MergingCriteriaSpecificService mergingCriteriaSpecificService

    // At the moment there is only this Criteria, therefore it is returned directly.
    // When new Criteria occur, they shall to be derived from the BamFile, which has to be merged.

    /**
     * @param bamFile, {@link ProcessedBamFile}, which shall be merged
     * @return a list of {@link MergingCriteria}, which must be used to create mergingSets for the given bamFile
     */
    List<MergingCriteria> criterias(ProcessedBamFile bamFile) {
        notNull(bamFile, "the input bam file for the method criterias is null")
        List<MergingCriteria> mergingCriterias = [MergingCriteria.DEFAULT]
        return mergingCriterias
    }

    /**
     * @param bamFile, {@link ProcessedBamFile}, which shell be merged
     * @param mergingCriteria, criteria, which has to be fulfilled by the other files to be merged to the input file
     * @return a list of {@link ProcessedBamFile}s, which fulfill the {@link MergingCriteria} and will be merged to the input file
     */
    List<ProcessedBamFile> bamFiles2Merge(ProcessedBamFile bamFile, MergingCriteria mergingCriteria) {
        notNull(bamFile, "the input bam file for the method bamFiles2Merge is null")
        notNull(mergingCriteria, "the input mergingCriteria for the method bamFiles2Merge is null")
        List<ProcessedBamFile> bamFiles2Merge = mergingCriteriaSpecificService."bamFilesForMergingCriteria${mergingCriteria}"(bamFile)
        bamFiles2Merge.each { ProcessedBamFile processedBamFile ->
            processedBamFile.status = State.INPROGRESS
            assertSave(processedBamFile)
        }
        return bamFiles2Merge
    }

    /**
     * @param workPackage, the mergedBamFile, which shall be merged again has to be produced with this workpackage
     * @param bamFile, the processed bam file, which needs to be merged
     * @param mergingCriteria, criteria, which has to be fulfilled by the {@link ProcessedMergedBamFile} to be merged to the input file
     * @return the processedMergedBamFile, which has to be merged with the processed bam file
     */
    ProcessedMergedBamFile mergedBamFile2Merge(MergingWorkPackage workPackage, ProcessedBamFile bamFile, MergingCriteria mergingCriteria) {
        notNull(workPackage, "the workPackage for the method mergedBamFiles2Merge is null")
        notNull(bamFile, "the bamFile for the method mergedBamFiles2Merge is null")
        notNull(mergingCriteria, "the input mergingCriteria for the method mergedBamFiles2Merge is null")
        ProcessedMergedBamFile processedMergedBamFile = mergingCriteriaSpecificService."mergedBamFileForMergingCriteria${mergingCriteria}"(workPackage, bamFile)
        if (processedMergedBamFile) {
            processedMergedBamFile.status = State.INPROGRESS
            assertSave(processedMergedBamFile)
        }
        return processedMergedBamFile
    }

    /**
     * @param mergingSet, for the {@link ProcessedBamFile}s in this mergingSet it is tested if they fulfill the {@link MergingCriteria}
     * @return true, if all BamFiles are valid, false otherwise
     */
    boolean validateBamFiles(MergingSet mergingSet) {
        notNull(mergingSet, "the input mergingSet for the method validateBamFiles is null")
        MergingCriteria mergingCriteria = mergingSet.mergingWorkPackage.mergingCriteria
        if (mergingSet.mergingWorkPackage.processingType == MergingWorkPackage.ProcessingType.MANUAL) {
            return true
        }
        return mergingCriteriaSpecificService."validateBamFilesForMergingCriteria${mergingCriteria}"(mergingSet)
    }

    private def assertSave(def object) {
        if (!object.save(flush: true)) {
            throw new SavingException(object.toString())
        }
        return object
    }
}
