package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State

class MergingCriteriaSpecificService {

    List<ProcessedBamFile> processedBamFilesForMerging(MergingWorkPackage workPackage) {
        List<ProcessedBamFile> bamFiles2Merge = []
        List<ProcessedBamFile> allBamFiles2Merge = ProcessedBamFile.createCriteria().list() {
            eq("type", BamType.SORTED)
            eq("withdrawn", false)
            //is used to make sure that only files, which were not merged already are used
            'in'("status", [State.NEEDS_PROCESSING, State.INPROGRESS])
            alignmentPass {
                eq("workPackage", workPackage)
            }
        }
        allBamFiles2Merge.each { ProcessedBamFile processedBamFile ->
            if (processedBamFile.isMostRecentBamFile()) {
                bamFiles2Merge.add(processedBamFile)
            }
        }
        return bamFiles2Merge
    }

    public ProcessedMergedBamFile processedMergedBamFileForMerging(MergingWorkPackage workPackage) {
        return ProcessedMergedBamFile.createCriteria().get {
            eq("type", BamType.MDUP)
            eq("withdrawn", false)
            mergingPass {
                mergingSet {
                    eq("status", MergingSet.State.PROCESSED)
                    eq("mergingWorkPackage", workPackage)
                    order("identifier", "desc")
                }
                order("identifier", "desc")
            }
            maxResults(1)
        }
    }
}
