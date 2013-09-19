package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.ngsdata.*

/**
 * each method in this class belongs to a specific MergingCriteria.
 * the {@link ProcessedBamFile}s, which fulfill this criteria are returned.
 *
 * !!the methods have all to be named the same way: bamFilesForMergingCriteria${MergingCriteria}!!
 *
 *
 */
class MergingCriteriaSpecificService {

    ProcessedBamFileService processedBamFileService

    AlignmentPassService alignmentPassService


    /**
     * @param bamFile, the {@link ProcessedBamFile}, which shall be merged
     * @return a list of {@link ProcessedBamFile}s, which fulfill the {@link MergingCriteria} and will be merged to the input file
     */
    List<ProcessedBamFile> bamFilesForMergingCriteriaDEFAULT(ProcessedBamFile bamFile) {
        return bamFilesForMergingCriteriaSeqTypeSamplePlatform(bamFile)
    }

    /**
     * @param bamFile, the {@link ProcessedBamFile}, which shall be merged
     * @return a list of {@link ProcessedBamFile}s, which fulfill the {@link MergingCriteria} and will be merged to the input file
     */
    List<ProcessedBamFile> bamFilesForMergingCriteriaSeqTypeSamplePlatform(ProcessedBamFile bamFile) {
        notNull(bamFile, "the input bam file for the method bamFilesForMergingCriteriaDEFAULT is null")
        List<ProcessedBamFile> bamFiles2Merge = []
        List<ProcessedBamFile> allBamFiles2Merge = ProcessedBamFile.createCriteria().list() {
            eq("type", BamType.SORTED)
            eq("withdrawn", false)
            alignmentPass {
                seqTrack {
                    eq("sample", bamFile.alignmentPass.seqTrack.sample)
                    eq("seqType", bamFile.alignmentPass.seqTrack.seqType)
                    seqPlatform {
                        eq("name", bamFile.alignmentPass.seqTrack.seqPlatform.name)
                    }
                }
            }
        }
        allBamFiles2Merge.each { ProcessedBamFile processedBamFile ->
            int maxIdentifier = alignmentPassService.maximalIdentifier(processedBamFile)
            if (processedBamFile.alignmentPass.identifier.equals(maxIdentifier)) {
                bamFiles2Merge.add(processedBamFile)
            }
        }
        return bamFiles2Merge
    }

    /**
     * @param mergingSet, {@link MergingSet} which has to be validated
     * @return true if the {@link ProcessedBamFile}s of the MergingSet fulfill the {@link MergingCriteria}
     */
    boolean validateBamFilesForMergingCriteriaDEFAULT(MergingSet mergingSet) {
        notNull(mergingSet, "the input mergingSet for the method validateBamFilesForMergingCriteriaDEFAULT is null")
        SeqPlatform seqPlatform = null
        List<ProcessedBamFile> processedBamFiles = processedBamFileService.findByMergingSet(mergingSet)
        for (bamFile in processedBamFiles) {
            SeqTrack seqTrack = bamFile.alignmentPass.seqTrack
            if (seqPlatform) {
                if (seqPlatform != seqTrack.seqPlatform) {
                    return false
                }
            } else {
                seqPlatform = seqTrack.seqPlatform
            }
            if (seqTrack.sample != mergingSet.mergingWorkPackage.sample) {
                return false
            }
            if (seqTrack.seqType != mergingSet.mergingWorkPackage.seqType) {
                return false
            }
        }
        return true
    }
}
