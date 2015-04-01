package de.dkfz.tbi.otp.dataprocessing

import static de.dkfz.tbi.otp.utils.CollectionUtils.*
import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.*
import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage.ProcessingType
import de.dkfz.tbi.otp.ngsdata.*

class MergingSetService {

    AbstractBamFileService abstractBamFileService

    ProcessedBamFileService processedBamFileService

    MergingCriteriaService mergingCriteriaService

    /**
     * It is checked if the {@link ProcessedBamFile} is valid for merging.
     * When it is valid, the corresponding MergingWorkPackage is determined or created, if it is not available.
     * In the end the bamFiles, which are from the same {@link MergingWorkPackage} are determined to merge
     * them to the {@link ProcessedBamFile}.
     * If there is a mergedBamFile, which has the same sample, seqType and platform name as the bamFile, it will also be merged.
     * For now, this workflow is only able to process workpackages of the type SYSTEM
     *
     * @param bamFile, the {@link ProcessedBamFile}, which shell be merged with other available BamFiles
     */
    void createMergingSetForBamFile(ProcessedBamFile bamFile) {
        notNull(bamFile, "the bam file is null")
        List<AbstractBamFile> bamFiles2Merge = []
        MergingWorkPackage workPackage = bamFile.mergingWorkPackage
        isTrue(workPackage.processingType.equals(ProcessingType.SYSTEM), "The processing type of this merging workpackage is not SYSTEM")
        ProcessedMergedBamFile mergedBamFile = mergingCriteriaService.processedMergedBamFileForMerging(workPackage)
        if (mergedBamFile) {
            bamFiles2Merge.add(mergedBamFile)
        }
        bamFiles2Merge.addAll(mergingCriteriaService.processedBamFilesForMerging(workPackage))
        if (bamFiles2Merge.empty) {
            threadLog?.info("There are no files to merge for MergingWorkPackage ${workPackage}")
            return
        }
        createMergingSet(bamFiles2Merge)
    }

    /**
     * creates {@link MergingSet} for the given list of
     * {@link ProcessedBamFile}s if such {@link MergingSet} does not exists
     * and assigns the bamFiles to this merging set
     *
     * @param bamFiles2Merge, List of {@link ProcessedBamFile}s, which shell be merged
     */
    void createMergingSet(List<AbstractBamFile> bamFiles2Merge) {
        notEmpty(bamFiles2Merge, "the input list of bam files for the method createMergingSet is empty")
        MergingWorkPackage workPackage = MergingWorkPackage.get(exactlyOneElement(bamFiles2Merge*.mergingWorkPackage*.id.unique()))
        if (bamFiles2Merge.size() == 1) {
            if (bamFiles2Merge.get(0) instanceof ProcessedMergedBamFile) {
                throw new RuntimeException('Trying to merge a single merged BAM file only. This makes no sense.')
            }
        }
        if (!checkIfMergingSetNotExists(bamFiles2Merge, workPackage)) {
            bamFiles2Merge.each { AbstractBamFile bamFile ->
                bamFile.status = State.PROCESSED
                assertSave(bamFile)
            }
            log.info("The merging set for ${workPackage} already exists".toString())
            return
        }
        MergingSet mergingSet = new MergingSet(identifier: MergingSet.nextIdentifier(workPackage), mergingWorkPackage: workPackage)
        assertSave(mergingSet)
        bamFiles2Merge.each {AbstractBamFile bamFile ->
            MergingSetAssignment mergingAssigment = new MergingSetAssignment(mergingSet: mergingSet, bamFile: bamFile)
            assertSave(mergingAssigment)
            abstractBamFileService.assignedToMergingSet(bamFile)
        }
        mergingSet.containedSeqTracks.each {    // The containedSeqTracks call itself does some useful validation.
            assert workPackage.satisfiesCriteria(it)
        }
        mergingSet.status = MergingSet.State.NEEDS_PROCESSING
        assertSave(mergingSet)
        log.debug("created a new mergingSet: ${mergingSet}")
    }

    /**
     * first it is checked if there are also other {@link MergingSet}s with the same {@link MergingWorkPackage} existing,
     * containing all {@link ProcessedBamFile}s, which shall be merged in this process.
     * second it has to be checked, if the found merging sets contain exactly the same number of bam files or more
     *
     * @param mergingSet, {@link MergingSet} which was created and shell be checked if it already exists
     * @return true if the merging set does not already exist
     */
    boolean checkIfMergingSetNotExists(List<AbstractBamFile> bamFiles2Merge, MergingWorkPackage workPackage) {
        notEmpty(bamFiles2Merge, "the input list of bam files for the method checkIfMergingSetNotExists is empty")
        notNull(workPackage, "the input work package for the method checkIfMergingSetNotExists is null")
        //determine all the merging sets, which include all the bam files, which shall be merged in this process
        //it was not possible to solve it via GORM, thats why HQL was used
        String query = """
            SELECT msa.mergingSet.id
            FROM MergingSetAssignment msa
            WHERE msa.bamFile IN (:bamFiles) AND msa.mergingSet.mergingWorkPackage =:workPackage
            GROUP BY msa.mergingSet.id
            HAVING count(msa.bamFile) =:number
        """
        List<Long> mergingSetIds= MergingSetAssignment.executeQuery(query, [bamFiles: bamFiles2Merge, workPackage: workPackage, number: bamFiles2Merge.size().longValue()])
        //determine, if the found merging sets have also other bam files than the once, which shall be merged in this process
        for (mergingSetId in mergingSetIds) {
            MergingSet mergingSet = MergingSet.get(mergingSetId)
            if (bamFiles2Merge.size().equals(mergingSet.bamFiles.size())) {
                return false
            }
        }
        return true
    }

    private def assertSave(def object) {
        if (!object.save(flush: true)) {
            throw new SavingException(object.toString())
        }
        return object
    }

    MergingSet mergingSetInStateNeedsProcessing() {
        return MergingSet.findByStatus(MergingSet.State.NEEDS_PROCESSING)
    }
}
