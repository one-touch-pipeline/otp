package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage.MergingCriteria
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage.ProcessingType
import de.dkfz.tbi.otp.dataprocessing.ProcessedBamFile.State
import de.dkfz.tbi.otp.job.processing.*

class MergingSetService {

    ProcessedBamFileService processedBamFileService

    MergingCriteriaService mergingCriteriaService

    MergingWorkPackageService mergingWorkPackageService

    /**
     * It is checked if the {@link ProcessedBamFile} is valid for merging.
     * When it is valid, the corresponding MergingWorkPackage is determined or created, if it is not available.
     * In the end the bamFiles, which are from the same {@link MergingWorkPackage} are determined to merge
     * them to the {@link ProcessedBamFile}.
     *
     * @param bamFile, the {@link ProcessedBamFile}, which shell be merged with other available BamFiles
     */
    void createMergingSetForBamFile(ProcessedBamFile bamFile) {
        notNull(bamFile, "the bam file is null")
        // In some cases it happens randomly that the status switches from "INPROGRESS" to "NEEDS_PROCESSING" between the start job and
        // the second job. Therefore the status is changed to "INPROGRESS" again, when the status is "NEEDS_PROCESSING"
        processedBamFileService.blockedForAssigningToMergingSet(bamFile)
        mergingCriteriaService.criterias(bamFile).each { MergingCriteria criteria ->
            Sample sample = bamFile.alignmentPass.seqTrack.sample
            SeqType seqType = bamFile.alignmentPass.seqTrack.seqType
            MergingWorkPackage workPackage = MergingWorkPackage.findBySampleAndSeqTypeAndMergingCriteria(sample, seqType, criteria)
            if (!workPackage) {
                workPackage = mergingWorkPackageService.createWorkPackage(sample, seqType, criteria)
            }
            isTrue(workPackage.processingType.equals(ProcessingType.SYSTEM), "The processing type of this merging workpackage is MANUAL")
            List<ProcessedBamFile> bamFiles2Merge = mergingCriteriaService.bamFiles2Merge(bamFile, criteria)
            if (bamFiles2Merge.empty) {
                log.info("There are no files to merge with the merging criteria ${criteria}")
                return
            }
            createMergingSet(bamFiles2Merge, workPackage)
        }
    }

    /**
     * creates {@link MergingSet} for the given list of
     * {@link ProcessedBamFile}s if such {@link MergingSet} does not exists
     * and assigns the bamFiles to this merging set
     *
     * @param bamFiles2Merge, List of {@link ProcessedBamFile}s, which shell be merged
     * @param workPackage, stores the merging information
     */
    void createMergingSet(List<ProcessedBamFile> bamFiles2Merge, MergingWorkPackage workPackage) {
        notEmpty(bamFiles2Merge, "the input list of bam files for the method createMergingSet is empty")
        notNull(workPackage, "the input work package for the method createMergingSet is null")
        if (!checkIfMergingSetNotExists(bamFiles2Merge, workPackage)) {
            bamFiles2Merge.each { ProcessedBamFile processedBamFile ->
                processedBamFile.status = State.PROCESSED
                assertSave(processedBamFile)
            }
            log.info("The merging set for the criteria ${workPackage.mergingCriteria} already exists".toString())
            return
        }
        MergingSet mergingSet = new MergingSet(identifier: nextIdentifier(workPackage), mergingWorkPackage: workPackage)
        assertSave(mergingSet)
        bamFiles2Merge.each {ProcessedBamFile bamFile ->
            MergingSetAssignment mergingAssigment = new MergingSetAssignment(mergingSet: mergingSet, bamFile: bamFile)
            assertSave(mergingAssigment)
            processedBamFileService.assignedToMergingSet(bamFile)
        }
        if (!mergingCriteriaService.validateBamFiles(mergingSet)) {
            throw new SavingException(mergingSet.toString())
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
    boolean checkIfMergingSetNotExists(List<ProcessedBamFile> bamFiles2Merge, MergingWorkPackage workPackage) {
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
            if (bamFiles2Merge.size().equals(processedBamFileService.findByMergingSet(mergingSet).size())) {
                return false
            }
        }
        return true
    }

    /**
     * @param workPackage, {@link MergingWorkPackage} for which the number of belonging {@link MergingSet}s shall be account
     * @return the number of MergingSets, which belong to this workPackage
     */
    Long nextIdentifier(MergingWorkPackage workPackage) {
        notNull(workPackage, "the input work package for the method nextIdentifier is null")
        return MergingSet.countByMergingWorkPackage(workPackage)
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
