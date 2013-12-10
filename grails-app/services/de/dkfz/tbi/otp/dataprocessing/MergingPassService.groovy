package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class MergingPassService {

    ConfigService configService

    MergingSetService mergingSetService

    MergingPass create() {
        MergingSet mergingSet = mergingSetService.mergingSetInStateNeedsProcessing()
        if (mergingSet) {
            int mergingPassCount = MergingPass.countByMergingSet(mergingSet)
            MergingPass mergingPass = new MergingPass(identifier: mergingPassCount, mergingSet: mergingSet)
            assertSave(mergingPass)
            log.debug("created a new mergingPass ${mergingPass} for mergingSet ${mergingSet}")
            return mergingPass
        }
        return null
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }

    public Realm realmForDataProcessing(MergingPass mergingPass) {
        notNull(mergingPass, "The parameter mergingPass is not allowed to be null")
        return configService.getRealmDataProcessing(project(mergingPass))
    }

    public Project project(MergingPass mergingPass) {
        notNull(mergingPass, "The parameter mergingPass is not allowed to be null")
        return mergingPass.mergingSet.mergingWorkPackage.sample.individual.project
    }

    public void mergingPassStarted(MergingPass mergingPass) {
        notNull(mergingPass, "The parameter mergingPass is not allowed to be null")
        updateMergingSet(mergingPass, MergingSet.State.INPROGRESS)
    }

    /**
     * After the merging is finished the state of the MergingSet is set to PROCESSED
     * and the mergedQA is triggered automatically.
     */
    public void mergingPassFinishedAndStartQA(MergingPass mergingPass) {
        notNull(mergingPass, "The parameter mergingPass is not allowed to be null")
        updateMergingSet(mergingPass, MergingSet.State.PROCESSED)
        mergedBamFileSetQaNotStarted(mergingPass)
    }

    private void updateMergingSet(MergingPass mergingPass, MergingSet.State state) {
        mergingPass.mergingSet.status = state
        assertSave(mergingPass.mergingSet)
    }

    /**
     * Change the qualityAssessmentStatus of the processedMergedBamFile, which belongs to the input mergingPass, to NOT_STARTED
     */
    public void mergedBamFileSetQaNotStarted(MergingPass mergingPass) {
        notNull(mergingPass, "The input of the method passNotStarted is null")
        ProcessedMergedBamFile processedMergedBamFile = ProcessedMergedBamFile.findByMergingPass(mergingPass)
        processedMergedBamFile.qualityAssessmentStatus = AbstractBamFile.QaProcessingStatus.NOT_STARTED
        assertSave(processedMergedBamFile)
    }
}
