package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
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

    SeqPlatformService seqPlatformService

    /**
     * @param bamFile, the {@link ProcessedBamFile}, which shall be merged
     * @return a list of {@link ProcessedBamFile}s, which fulfill the {@link MergingCriteria} and will be merged to the input file
     */
    List<ProcessedBamFile> bamFilesForMergingCriteriaDEFAULT(ProcessedBamFile bamFile) {
        notNull(bamFile, "the input bam file for the method bamFilesForMergingCriteriaDEFAULT is null")
        return bamFilesForMergingCriteriaSeqTypeSamplePlatform(bamFile)
    }

    /**
     * @param bamFile, the {@link ProcessedBamFile}, which shall be merged
     * @return a list of {@link ProcessedBamFile}s, which fulfill the {@link MergingCriteria} and will be merged to the input file
     */
    private List<ProcessedBamFile> bamFilesForMergingCriteriaSeqTypeSamplePlatform(ProcessedBamFile bamFile) {
        List<ProcessedBamFile> bamFiles2Merge = []
        List<ProcessedBamFile> allBamFiles2Merge = ProcessedBamFile.createCriteria().list() {
            eq("type", BamType.SORTED)
            eq("withdrawn", false)
            //is used to make sure that only files, which were not merged already are used
            eq("status", State.NEEDS_PROCESSING)
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
        allBamFiles2Merge.add(bamFile)
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
        String seqPlatform
        List<AbstractBamFile> bamFiles = processedBamFileService.findByMergingSet(mergingSet)
        for (bamFile in bamFiles) {
            if (bamFile instanceof ProcessedBamFile) {
                //processed bam files
                SeqTrack seqTrack = bamFile.alignmentPass.seqTrack
                if (seqPlatform) {
                    if (seqPlatform != seqTrack.seqPlatform.name) {
                        return false
                    }
                } else {
                    seqPlatform = seqTrack.seqPlatform.name
                }
                if (seqTrack.sample != mergingSet.mergingWorkPackage.sample) {
                    return false
                }
                if (seqTrack.seqType != mergingSet.mergingWorkPackage.seqType) {
                    return false
                }
            } else if(bamFile instanceof ProcessedMergedBamFile) {
                //processed merged bam file
                MergingWorkPackage workPackage = bamFile.mergingPass.mergingSet.mergingWorkPackage
                if (workPackage != mergingSet.mergingWorkPackage) {
                    return false
                }
                String seqPlatformMergedFile = seqPlatformService.platformForMergedBamFile(bamFile).name
                if (seqPlatform) {
                    if (seqPlatform != seqPlatformMergedFile) {
                        return false
                    }
                } else {
                    seqPlatform = seqPlatformMergedFile
                }
            } else {
                throw new Exception("The BamFile '${bamFile}' is neither a ProcessedBamFile, nor a ProcessedMergedBamFile")
            }
        }
        return true
    }

    /**
     * @param workPackage, the mergedBamFile, which shall be merged again has to be produced with this workpackage
     * @param bamFile, the processed bam file, which needs to be merged
     * @return the processedMergedBamFile, which has to be merged with the processed bam file
     */
    ProcessedMergedBamFile mergedBamFileForMergingCriteriaDEFAULT(MergingWorkPackage workPackage, ProcessedBamFile bamFile) {
        notNull(workPackage, "the workPackage for the method mergedBamFileForMergingCriteriaDEFAULT is null")
        notNull(bamFile, "the bamFile for the method mergedBamFileForMergingCriteriaDEFAULT is null")
        return mergedBamFileForMergingCriteriaSeqTypeSamplePlatform(workPackage, bamFile)
    }

    /**
     * This method bases on the assumption, which was made in the beginning of this workflow in the ProcessedBamFileService:
     * Only one mergingSet is created for one sample and seqType in parallel
     *
     * @param workPackage, the mergedBamFile, which shall be merged again has to be produced with this workpackage
     * @param bamFile, the processed bam file, which needs to be merged
     * @return the processedMergedBamFile, which has to be merged with the processed bam file
     */
    private ProcessedMergedBamFile mergedBamFileForMergingCriteriaSeqTypeSamplePlatform(MergingWorkPackage workPackage, ProcessedBamFile bamFile) {
        String platform = bamFile.alignmentPass.seqTrack.seqPlatform.name
        List<ProcessedMergedBamFile> mergedBamFiles = ProcessedMergedBamFile.createCriteria().list {
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
        }
        for (ProcessedMergedBamFile processedMergedBamFile : mergedBamFiles) {
            String platformMergedBamFile = seqPlatformService.platformForMergedBamFile(processedMergedBamFile).name
            if (platformMergedBamFile.equals(platform)) {
                return processedMergedBamFile
            }
        }
        return null
    }
}
