package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*
import grails.validation.*
import org.springframework.security.access.prepost.*
import org.springframework.validation.*

import static org.springframework.util.Assert.*

class MergingCriteriaService {

    MergingCriteriaSpecificService mergingCriteriaSpecificService

    /**
     * Finds {@link ProcessedBamFile}s to be merged and sets their {@link ProcessedBamFile#status} to
     * {@link State#INPROGRESS}.
     */
    List<ProcessedBamFile> processedBamFilesForMerging(MergingWorkPackage workPackage) {
        notNull(workPackage)
        List<ProcessedBamFile> bamFiles2Merge = mergingCriteriaSpecificService.processedBamFilesForMerging(workPackage)
        bamFiles2Merge.each { ProcessedBamFile processedBamFile ->
            processedBamFile.status = State.INPROGRESS
            assertSave(processedBamFile)
        }
        return bamFiles2Merge
    }

    /**
     * Finds the {@link ProcessedMergedBamFile}s to be merged and sets its {@link ProcessedMergedBamFile#status} to
     * {@link State#INPROGRESS}. May return {@code null}
     */
    ProcessedMergedBamFile processedMergedBamFileForMerging(MergingWorkPackage workPackage) {
        notNull(workPackage, "the workPackage for the method mergedBamFiles2Merge is null")
        ProcessedMergedBamFile processedMergedBamFile =
                mergingCriteriaSpecificService.processedMergedBamFileForMerging(workPackage)
        if (processedMergedBamFile) {
            processedMergedBamFile.status = State.INPROGRESS
            assertSave(processedMergedBamFile)
        }
        return processedMergedBamFile
    }

    private def assertSave(def object) {
        if (!object.save(flush: true)) {
            throw new SavingException(object.toString())
        }
        return object
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    MergingCriteria findMergingCriteria(Project project, SeqType seqType) {
        return MergingCriteria.findByProjectAndSeqType(project, seqType) ?: new MergingCriteria()
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    Errors createOrUpdateMergingCriteria(Project project, SeqType seqType, boolean libPrepKit, MergingCriteria.SpecificSeqPlatformGroups seqPlatformGroups) {
        MergingCriteria mergingCriteria = MergingCriteria.findOrCreateWhere(project: project, seqType: seqType)
        mergingCriteria.libPrepKit = libPrepKit
        mergingCriteria.seqPlatformGroup = seqPlatformGroups
        try {
            mergingCriteria.save(flush: true)
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }
}
