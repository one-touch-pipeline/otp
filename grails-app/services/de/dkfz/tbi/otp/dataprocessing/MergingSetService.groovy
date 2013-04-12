package de.dkfz.tbi.otp.dataprocessing

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.Assert

import grails.gorm.DetachedCriteria
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.job.processing.*

class MergingSetService {

    @Autowired
    ProcessedBamFileService processedBamFileService

    @Autowired
    MergingCriteriaService mergingCriteriaService

    @Autowired
    MergingWorkPackageService mergingWorkPackageService

    private final String getByBamFilesList = "select ma.mergingSet from MergingSetAssigment ma group by ma.mergingSet having ma.bamFile in (:bamFiles) and count(ma.bamFile) == :count" // TODO: to be proved

    public void createMergingSet(long bamFileId) {
        Assert.notNull(bamFileId)
        ProcessedBamFile bamFile = ProcessedBamFile.getById(bamFileId)
        Assert.notNull(bamFile) // really exists in the db
        Assert.isTrue(processedBamFileService.isFree(bamFile))
        Assert.isTrue(processedBamFileService.isValid4Merging(bamFile))

        mergingCriteriaService.getCriterias(bamFile).each {criteria ->
            Sample sample = bamFile.alignmentPass.seqTrack.sample
            MergingWorkPackage workPackage = MergingWorkPackage.findBySampleAndMergingCriteria(sample, criteria)
            if (!workPackage) {
                workPackage = mergingWorkPackageService.createWorkPackge(sample, criteria)
            }
            List<ProcessedBamFile> bamFiles2Merge = mergingCriteriaService.getBamFiles2Merge(bamFile, criteria)
            createMergingSet(bamFiles2Merge, workPackage)
        }
    }

    /**
     * creates {@link MergingSet} for the given list of
     * {@link ProcessedBamFile}s if such {@link MergingSet} does not exists
     */
    public void createMergingSet(List<ProcessedBamFile> bamFiles2Merge, MergingWorkPackage workPackage) {
        Assert.notEmpty(bamFiles2Merge)
        Assert.notNull(workPackage)
        MergingSet existingMergingSet = MergingSetAssignment.find(getByBamFilesList) // TODO: apply params
        if (existingMergingSet) {
            log.debug("merging set for the given set of bam file already exists: ${existingMergingSet}")
            return
        }
        MergingSet mergingSet = new MergingSet(identifier: getNextIdentifier(sample), mergingWorkPackage: workPackage)
        assertSave(mergingSet)
        bamFiles2Merge.each {ProcessedBamFile bamFile ->
            MergingSetAssignment mergingAssigment = new MergingSetAssignment(mergingSet: mergingSet, bamFile: bamFile)
            assertObjectSave(mergingAssigment)
        }
        mergingSet.status = MergingSet.State.NEEDS_PROCESSING
        assertSave(mergingSet)
        log.debug("created a new mergingSet: ${mergingSet}")
    }

    Long getNextIdentifier(MergingWorkPackage workPackage) {
        MergingSet.countByMergingWorkPackage(workPackage)
    }

    private MergingSet assertSave(MergingSet mergingSet) {
        if (!mergingCriteriaService.validateBamFiles(mergingSet)) {
            throw new SavingException(mergingSet.toString())
        }
        assertObjectSave(mergingSet)
    }

    private def assertObjectSave(def object) {
        if (!object.save(flush: true)) {
            throw new SavingException(object.toString())
        }
        return object
    }

    MergingSet getNextMergingSet() {
        return MergingSet.findByStatus(MergingSet.State.DECLARED)
    }

    void blockForMerging(MergingSet mergingSet) {
        mergingSet.status = MergingSet.State.INPROGRESS
        assertObjectSave(mergingSet)
    }

    public void mergingSetFinished(MergingSet mergingSet) {
        mergingSet.status = MergingSet.State.PROCESSED
        assertObjectSave(mergingPass)
    }

}
