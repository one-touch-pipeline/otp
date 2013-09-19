package de.dkfz.tbi.otp.dataprocessing

import static org.springframework.util.Assert.*
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.BamType
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile.State
import de.dkfz.tbi.otp.ngsdata.*

class ProcessedBamFileService {

    ProcessedAlignmentFileService processedAlignmentFileService
    ConfigService configService
    AbstractBamFileService abstractBamFileService

    public String getFilePath(ProcessedBamFile bamFile) {
        String dir = getDirectory(bamFile)
        String filename = getFileName(bamFile)
        return "${dir}/${filename}"
    }

    public String getFilePathNoSuffix(ProcessedBamFile bamFile) {
        String dir = getDirectory(bamFile)
        String filename = getFileNameNoSuffix(bamFile)
        return "${dir}/${filename}"
    }

    /**
     * Retrieves the path to a log file used by bwa sampe
     * (Although is not Philosophy of OTP to keep track of log files,
     * it is required by bwa since it produces not empty output files
     * even when it fails, and so we need to analyse the log file contents too)
     *
     * @param saiFile processed bam file object
     * @return Path to the outputted error file produced by bwa sampe
     */
    public String bwaSampeErrorLogFilePath(ProcessedBamFile bamFile) {
        return "${getFilePath(bamFile)}_bwaSampeErrorLog.txt"
    }

    public String baiFilePath(ProcessedBamFile bamFile) {
        return "${getFilePath(bamFile)}.bai"
    }

    public String getDirectory(ProcessedBamFile bamFile) {
        return processedAlignmentFileService.getDirectory(bamFile.alignmentPass)
    }

    public String getFileName(ProcessedBamFile bamFile) {
        String body = getFileNameNoSuffix(bamFile)
        return "${body}.bam"
    }

    public String getFileNameNoSuffix(ProcessedBamFile bamFile) {
        SeqTrack seqTrack = bamFile.alignmentPass.seqTrack
        String sampleType = seqTrack.sample.sampleType.name.toLowerCase()
        String runName = seqTrack.run.name
        String lane = seqTrack.laneId
        String layout = seqTrack.seqType.libraryLayout
        String suffix = ""
        switch (bamFile.type) {
            case AbstractBamFile.BamType.SORTED:
                suffix = ".sorted"
                break
            case AbstractBamFile.BamType.RMDUP:
                suffix = ".sorted.rmdup"
                break
            case AbstractBamFile.BamType.MDUP:
                suffix = ".sorted.mdup"
                break
        }
        return "${sampleType}_${runName}_s_${lane}_${layout}${suffix}"
    }

    public ProcessedBamFile createSortedBamFile(AlignmentPass alignmentPass) {
        return createBamFile(alignmentPass, AbstractBamFile.BamType.SORTED)
    }

    private ProcessedBamFile createBamFile(AlignmentPass alignmentPass, AbstractBamFile.BamType type) {
        ProcessedBamFile pbf = new ProcessedBamFile(
                        alignmentPass: alignmentPass,
                        type: type
                        )
        assert(pbf.save(flush: true))
        return pbf
    }

    public ProcessedBamFile findSortedBamFile(AlignmentPass alignmentPass) {
        def type = AbstractBamFile.BamType.SORTED
        return findBamFile(alignmentPass, type)
    }

    private ProcessedBamFile findBamFile(AlignmentPass alignmentPass, AbstractBamFile.BamType type) {
        return ProcessedBamFile.findByAlignmentPassAndType(alignmentPass, type)
    }

    public ProcessedBamFile findBamFile(long alignmentPassId, String type) {
        AlignmentPass alignmentPass = AlignmentPass.get(alignmentPassId)
        return ProcessedBamFile.findByAlignmentPassAndType(alignmentPass, type)
    }

    public boolean updateBamFile(ProcessedBamFile bamFile) {
        File file = new File(getFilePath(bamFile))
        if (!file.canRead()) {
            return false
        }
        bamFile.fileExists = true
        bamFile.fileSize = file.length()
        bamFile.dateFromFileSystem = new Date(file.lastModified())
        assertSave(bamFile)
        return bamFile.fileSize
    }

    public boolean updateBamFileIndex(ProcessedBamFile bamFile) {
        String path = baiFilePath(bamFile)
        File file = new File(path)
        if (!file.canRead()) {
            return false
        }
        bamFile.hasIndexFile = true
        assertSave(bamFile)
        return true
    }

    public Realm realm(ProcessedBamFile processedBamFile) {
        Project project = project(processedBamFile)
        return configService.getRealmDataProcessing(project)
    }

    public Project project(ProcessedBamFile processedBamFile) {
        return processedBamFile.alignmentPass.seqTrack.sample.individual.project
    }

    public SeqType seqType(ProcessedBamFile processedBamFile) {
        return processedBamFile.alignmentPass.seqTrack.seqType
    }

    public SeqTrack seqTrack(ProcessedBamFile processedBamFile) {
        return processedBamFile.alignmentPass.seqTrack
    }

    public Sample sample(ProcessedBamFile processedBamFile) {
        return processedBamFile.alignmentPass.seqTrack.sample
    }

    /**
     * @return the first available {@link ProcessedBamFile}, which needs to be merged and was sorted in the alignment workflow
     */
    ProcessedBamFile processedBamFileNeedsProcessing() {
        //the oldest bam file will be processed
        List<ProcessedBamFile> processedBamFiles = ProcessedBamFile.findAllByStatusAndTypeAndWithdrawn(State.NEEDS_PROCESSING, BamType.SORTED, false, [sort: "id"])
        for (ProcessedBamFile processedBamFile : processedBamFiles) {
            Sample sample = processedBamFile.alignmentPass.seqTrack.sample
            SeqType seqType = processedBamFile.alignmentPass.seqTrack.seqType
            //waiting until all fastq files from the same sample and seqtype are aligned so that they can be merged all together
            List<SeqTrack> seqTracks = SeqTrack.createCriteria().list {
                eq("sample", sample)
                eq("seqType", seqType)
                'in'("alignmentState", [SeqTrack.DataProcessingState.NOT_STARTED, SeqTrack.DataProcessingState.IN_PROGRESS])
            }
            if (seqTracks.isEmpty()) {
                MergingWorkPackage workPackage = MergingWorkPackage.findBySampleAndSeqType(sample, seqType)
                if (workPackage) {
                    //make sure that there is no other merging process with the same sample and seqtype running at the moment
                    MergingSet mergingSet = MergingSet.findByMergingWorkPackageAndStatusInList(workPackage,
                                    [MergingSet.State.DECLARED, MergingSet.State.INPROGRESS, MergingSet.State.NEEDS_PROCESSING])
                    if (!mergingSet) {
                        return processedBamFile
                    }
                } else {
                    return processedBamFile
                }
            }
        }
    }

    /**
     * @param bamFile, which is in progress to be assigned to a {@link MergingSet}
     */
    void blockedForAssigningToMergingSet(ProcessedBamFile bamFile) {
        notNull(bamFile, "the input bam file for the method blockedForAssigningToMergingSet is null")
        bamFile.status = State.INPROGRESS
        assertSave(bamFile)
    }

    /**
     * @param bamFile, {@link ProcessedBamFile}, which shall be merged
     * @return true, if the bamFile is not already assigned to a MergingSet, otherwise false
     */
    boolean notAssignedToMergingSet(ProcessedBamFile bamFile) {
        notNull(bamFile, "the input bam file for the method notAssignedToMergingSet is null")
        return !MergingSetAssignment.findByBamFile(bamFile)
    }

    private def assertSave(def object) {
        object = object.save(flush: true)
        if (!object) {
            throw new SavingException(object.toString())
        }
        return object
    }
}
