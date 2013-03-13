package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.DetachedCriteria
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage.MergingCriteria
import de.dkfz.tbi.otp.ngsdata.*


class MergingCriteriaService {

    // do not go deep into a good design
    List<MergingCriteria> getCriterias(ProcessedBamFile bamFile) {
        MergingCriteria.DEFAULT
    }

    // do not go deep into a good design
    List<ProcessedBamFile> getBamFiles2Merge(ProcessedBamFile bamFile, MergingCriteria criteria) {
        if (criteria == MergingCriteria.DEFAULT) {
            DetachedCriteria latestAlignmentPasses = new DetachedCriteria(AlignmentPass).build {
                projections {
                    groupProperty("seqTrack")
                    max("identifier")
                }
            }
            List<ProcessedBamFile> bamFiles2Merge = ProcessedBamFile.createCriteria().list {
                eq("type", BamType.RMDUP)
                eq("qualityControl", AbstractBamFile.QualityControl.PASSED)
                alignmentPass {
                    "in" ("id", latestAlignmentPasses.list())
                    eq("status", AlignmentPass.State.SUCCEED)
                    seqTrack {
                        eq("isSkipped", false)
                        eq("sample", procBamFile.alignmentPass.seqTrack.sample)
                        eq("seqPlatform", procBamFile.alignmentPass.seqTrack.seqPlatform)
                        eq("seqType", procBamFile.alignmentPass.seqTrack.seqType)
                    }
                }
            }
            return bamFiles2Merge
        }
    }

    // do not go deep into a good design and coding style
    boolean validateBamFiles(MergingSet mergingSet) {
        MergingCriteria criteria = mergingSet.mergingWorkPackage.mergingCriteria
        if (criteria == MergingCriteria.DEFAULT) {
            if (mergingSet.mergingWorkPackage.processingType == MergingWorkPackage.ProcessingType.MANUAL) {
                return true
            }
            for (ProcessedBamFile bamFile in MergingSetAssignment.findAllByMergingSet(mergingSet)*.bamFile) {
                SeqTrack seqTrack = bamFile.alignmentPass.seqTrack
                if (seqTrack.sample != mergingSet.sample) {
                    return false
                }
                if (seqTrack.seqType != mergingSet.seqType) {
                    return false
                }
                if (seqTrack.seqPlatform != mergingSet.seqPlatform) {
                    return false
                }
            }
            return true
        }
    }
}
